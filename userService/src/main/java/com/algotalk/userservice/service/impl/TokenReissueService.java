package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.response.TokenReissueResponseDTO;
import com.algotalk.userservice.repository.IUserLoginMapper;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IRefreshTokenService;
import com.algotalk.userservice.service.ITokenReissueService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import static com.algotalk.userservice.exception.UserErrorCode.*;

/**
 * 쿠키의 기존 RT를 검증하고 새 AT/RT를 발급하는 서비스다.
 * 마지막 RT 교체 단계는 Redis CAS 결과가 성공일 때에만 응답 쿠키를 만든다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenReissueService implements ITokenReissueService {

    private final IJwtTokenService jwtTokenService;
    private final IRefreshTokenService refreshTokenService;
    private final IUserLoginMapper userLoginMapper;

    @Value("${jwt.access.token.expiration}")
    private Long accessTokenExpiration;

    @Value("${cookie.access.name}")
    private String accessCookieName;

    @Value("${cookie.refresh.name}")
    private String refreshCookieName;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String sameSite;

    @Value("${jwt.refresh.token.expiration}")
    private Long refreshTokenExpiration;

    @Override
    public TokenReissueResponseDTO reissueToken(HttpServletRequest request,
                                                HttpServletResponse response) throws Exception {

        // 1. Cookie에서 Refresh Token 추출
        String refreshToken = extractRefreshTokenFromCookie(request);
        if(refreshToken == null || refreshToken.isBlank()) {
            // Refresh Token이 없거나 빈 값인 경우 예외 처리
            log.warn("Refresh Token이 쿠키에 존재하지 않거나 비어 있습니다.");
            throw new BusinessException(REFRESH_TOKEN_NOT_FOUND);
        }

        // 2. Refresh Token Decode해서 userId 추출
        Long userId; // userId 추출 실패 했을 때 예외 처리 위해 try-catch로 감싸기
        try {
            userId = jwtTokenService.getUserIdFromToken(refreshToken);
            log.debug("Refresh Token에서 userId 추출 완료");
        } catch (Exception e) {
            // decode 실패 시 예외 처리 (유효하지 않은 토큰)
            log.warn("유효하지 않은 Refresh Token입니다. class={}", e.getClass().getSimpleName());
            throw new BusinessException(TOKEN_INVALID);
        }

        // 3. Redis에 RT가 없거나 이미 다른 값이면 DB 조회와 토큰 생성을 하지 않고 빠르게 실패한다.
        // 이 조회만으로 교체를 확정하지 않으며, 실제 저장 직전에 Redis CAS가 같은 조건을 다시 검사한다.
        String storedRefreshToken = refreshTokenService.getRefreshToken(userId);
        if (storedRefreshToken == null) {
            log.warn("Redis에 저장된 Refresh Token이 존재하지 않습니다.");
            throw new BusinessException(TOKEN_EXPIRED);
        }
        if (!storedRefreshToken.equals(refreshToken)) {
            log.warn("제공된 Refresh Token이 저장된 토큰과 일치하지 않습니다.");
            throw new BusinessException(TOKEN_MISMATCH);
        }

        // 4. DB에서 사용자 정보를 조회(Access Token 재발급할 때 필요)
        UserInfoCommand rCommand = userLoginMapper.getUserAuthInfo(
                UserInfoCommand.builder().userId(userId).build()
        );

        if(rCommand == null) {
            log.warn("토큰 재발급 대상 사용자 정보를 DB에서 조회할 수 없습니다.");
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 5. 새로운 Access Token과 Refresh Token 생성
        String newAccessToken = jwtTokenService.generateAccessToken(rCommand);
        String newRefreshToken = jwtTokenService.generateRefreshToken(rCommand);

        // 6. 기존 RT 비교와 새 RT 저장을 Redis에서 원자적으로 수행한다.
        // 사전 조회 이후 다른 요청이 먼저 RTR을 완료했다면 여기서 MISMATCH가 되어 중복 성공을 막는다.
        IRefreshTokenService.RotationResult rotationResult =
                refreshTokenService.rotateRefreshToken(userId, refreshToken, newRefreshToken);
        // Redis key가 만료되거나 삭제된 경우이므로 RT 만료로 응답한다.
        if (rotationResult == IRefreshTokenService.RotationResult.NOT_FOUND) {
            log.warn("Redis에 저장된 Refresh Token이 존재하지 않습니다.");
            throw new BusinessException(TOKEN_EXPIRED);
        }
        // 다른 요청이 먼저 회전했거나 오래된 RT가 재사용된 경우이므로 불일치로 응답한다.
        if (rotationResult == IRefreshTokenService.RotationResult.MISMATCH) {
            log.warn("제공된 Refresh Token이 저장된 토큰과 일치하지 않습니다.");
            throw new BusinessException(TOKEN_MISMATCH);
        }

        // 7. CAS가 성공한 뒤에만 새 RT를 쿠키에 내려 Redis 값과 브라우저 값을 맞춘다.
        setRefreshTokenCookie(newRefreshToken, response); // 기존 쿠키 삭제 및 새로운 쿠키 설정

        // 8. 새로운 Access Token을 헤더 및 쿠키에 담아서 Response에 추가
        setAccessTokenHeader(newAccessToken, response);
        setAccessTokenCookie(newAccessToken, response);

        // 9. 토큰 메타 정보만 Response DTO로 반환
        TokenReissueResponseDTO rDTO = TokenReissueResponseDTO.builder()
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // ms -> 초 변환
                .build();

        return rDTO;
    }

    // 토큰 추출 함수
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        log.info("{}.extractRefreshTokenFromCookie Start!", this.getClass().getName());
        // 쿠키에서 Refresh Token 추출 로직 구현
        if(request.getCookies() == null) {
            return null; // 쿠기에 토큰 없으면 null 반환
        }

        // 1. 쿠키 배열에서 refreshCookieName과 일치하는 쿠키 찾기
        // 2. 해당 쿠키가 존재하면 쿠키의 값을 반환(Refresh Token)
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if(refreshCookieName.equals(cookie.getName())) {
               log.info("{}.extractRefreshTokenFromCookie End!", this.getClass().getName());
               return  cookie.getValue();
            }
        }

        log.info("{}.extractRefreshTokenFromCookie End!", this.getClass().getName());
        return null;
    }

    private void setAccessTokenHeader(String accessToken, HttpServletResponse response) {
        log.info("{}.setAccessTokenHeader Start!", this.getClass().getName());
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        log.info("{}.setAccessTokenHeader End!", this.getClass().getName());
    }

    // 토큰 삭제 및 저장 함수
    private void setAccessTokenCookie(String accessToken, HttpServletResponse response) {
        log.info("{}.setAccessTokenCookie Start!", this.getClass().getName());

        // 새로운 Refresh Token이 저장된 쿠키 생성 및 추가
        ResponseCookie cookie = ResponseCookie.from(accessCookieName, accessToken)
                .httpOnly(true) // 항상 true (XSS 방어)
                .secure(cookieSecure) // yml 설정값 사용
                .path("/")
                .sameSite(sameSite) // yml 설정값 사용
//                .maxAge(accessTokenExpiration / 1000) // ms -> 초 변환
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("{}.setAccessTokenCookie End!", this.getClass().getName());
    }

    // 토큰 삭제 및 저장 함수
    private void setRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
        log.info("{}.setRefreshTokenCookie Start!", this.getClass().getName());

        // 새로운 Refresh Token이 저장된 쿠키 생성 및 추가
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true) // 항상 true (XSS 방어)
                .secure(cookieSecure) // yml 설정값 사용
                .path("/")
                .sameSite(sameSite) // yml 설정값 사용
                .maxAge(refreshTokenExpiration / 1000) // ms -> 초 변환
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("{}.setRefreshTokenCookie End!", this.getClass().getName());
    }
}
