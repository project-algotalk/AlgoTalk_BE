package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.auth.RefreshTokenIssue;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.dto.response.LoginResponseDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IUserLoginMapper;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IRefreshTokenService;
import com.algotalk.userservice.service.IUserLoginService;
import com.algotalk.userservice.util.CmmUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginService implements IUserLoginService {

    private final IUserLoginMapper userLoginMapper;
    private final IJwtTokenService jwtTokenService;
    private final IRefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String LOGIN_FAIL_KEY = "login:fail:"; // 로그인 실패 횟수
    private static final String LOGIN_LOCK_KEY = "login:lock:"; // 로그인 잠금 여부

    @Value("${login.max-fail-count}")
    private int maxFailCount;

    @Value("${login.lock-minutes}")
    private long lockMinutes; // 로그인 잠금 시간

    @Value("${jwt.access.token.expiration}")
    private long accessTokenExpiration;

    @Value("${cookie.access.name}")
    private String accessCookieName;

    @Value("${cookie.refresh.name}")
    private String refreshCookieName;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String sameSite;

    @Override
    public void login(LoginRequestDTO pDTO, HttpServletResponse response) throws Exception {
        log.info("{}.login Start!", this.getClass().getName());

        String loginId = CmmUtil.nvl(pDTO.loginId());

        // 1. 계정 잠금 확인
        if (isAccountLocked(loginId)) {
            log.warn("계정이 잠금 상태입니다: loginId={}", loginId);
            throw new BusinessException(UserErrorCode.ACCOUNT_LOCKED);
        }

        // 2. 사용자 정보 조회
        UserInfoCommand rCommand = userLoginMapper.getUserAuthInfo(
                UserInfoCommand.builder()
                        .loginId(loginId)
                        .build()
        );

        // 3. 사용자 존재 여부 확인
        if (rCommand == null) {
            log.warn("사용자 정보가 존재하지 않습니다: loginId={}", loginId);
            throw new BusinessException(UserErrorCode.USER_NOT_FOUND);
        }

        // 4. 비밀번호 검증
        if(!passwordEncoder.matches(pDTO.password(), rCommand.getPassword())) {
            log.warn("비밀번호가 일치하지 않습니다: loginId={}", loginId);
            // 로그인 실패 횟수 증가 및 잠금 처리
            handleLoginFail(loginId);
            throw new BusinessException(UserErrorCode.LOGIN_FAIL);
        }

        // 6. 로그인 성공 시 로그인 실패 횟수 초기화
        stringRedisTemplate.delete(LOGIN_FAIL_KEY + loginId);

        // 7. 로그인 세션을 먼저 생성하고 Access Token에도 동일한 sessionId를 포함
        RefreshTokenIssue refreshTokenIssue = jwtTokenService.issueRefreshToken(rCommand);
        String accessToken = jwtTokenService.generateAccessToken(rCommand, refreshTokenIssue.sessionId());

        // 8. 로그인 세션별 Refresh Token Redis 저장
        refreshTokenService.saveRefreshToken(
                rCommand.getUserId(),
                refreshTokenIssue.sessionId(),
                refreshTokenIssue.token(),
                refreshTokenIssue.expiresAt()
        );

        // 9. 실제 RT 만료 시각에 맞춰 쿠키 설정
        setRefreshTokenCookie(refreshTokenIssue.token(), refreshTokenIssue.expiresAt(), response);

        // 10. Access Token 헤더 설정
//        setAccessTokenHeader(accessToken, response);
        setAccessTokenCookie(accessToken, response);

        log.info("{}.login End!", this.getClass().getName());
    }

    @Override
    public void logout(Long userId, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.info("{}.logout Start!", this.getClass().getName());

        // 쿠키의 RT가 가리키는 현재 로그인 세션만 폐기
        String refreshToken = extractRefreshTokenFromCookie(request);
        if (refreshToken != null) {
            try {
                Long tokenUserId = jwtTokenService.getUserIdFromToken(refreshToken);
                String sessionId = jwtTokenService.getSessionIdFromToken(refreshToken);
                if (userId.equals(tokenUserId) && sessionId != null) {
                    refreshTokenService.deleteRefreshToken(userId, sessionId);
                }
            } catch (Exception e) {
                log.warn("로그아웃 요청의 Refresh Token을 해석할 수 없어 쿠키만 삭제합니다.");
            }
        }

        // 클라이언트 인증 쿠키 삭제
        expireAccessTokenCookie(response);
        expireRefreshTokenCookie(response);

        log.info("{}.logout End!", this.getClass().getName());

    }

    @Override
    public void logoutAll(Long userId, HttpServletResponse response) throws Exception {
        log.info("{}.logoutAll Start!", this.getClass().getName());

        refreshTokenService.deleteAllRefreshTokens(userId);
        expireAccessTokenCookie(response);
        expireRefreshTokenCookie(response);

        log.info("{}.logoutAll End!", this.getClass().getName());
    }

    private boolean isAccountLocked(String loginId) {
        log.info("{}.isAccountLocked Start!", this.getClass().getName());
        String lockKey = LOGIN_LOCK_KEY + loginId;

        log.info("{}.isAccountLocked End!", this.getClass().getName());
        return stringRedisTemplate.hasKey(lockKey);
    }

    private void handleLoginFail(String loginId) {
        String failKey = LOGIN_FAIL_KEY + loginId;

        // 실패 횟수 증가
        Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
        log.info("로그인 실패 횟수 증가: key={}, failCount={}", failKey, failCount);
        stringRedisTemplate.expire(failKey, lockMinutes, TimeUnit.MINUTES);

        // 최대 실패 횟수 초과 시 계정 잠금
        if(failCount != null && failCount >= maxFailCount) {
            String lockKey = LOGIN_LOCK_KEY + loginId;
            stringRedisTemplate.opsForValue().set(lockKey, "Y", lockMinutes, TimeUnit.MINUTES);
            log.warn("계정 잠금 처리: key={}, lockMinutes={}분", lockKey, lockMinutes);
        }
    }

    private void setAccessTokenHeader(String accessToken, HttpServletResponse response) {
        log.info("{}.setAccessTokenHeader Start!", this.getClass().getName());
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        log.info("{}.setAccessTokenHeader End!", this.getClass().getName());
    }

    private void setAccessTokenCookie(String accessToken, HttpServletResponse response) {
        log.info("{}.setAccessTokenCookie Start!", this.getClass().getName());

        ResponseCookie cookie = ResponseCookie.from(accessCookieName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(sameSite)
//                .maxAge(accessTokenExpiration / 1000)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("{}.setAccessTokenCookie End!", this.getClass().getName());
    }

    private void expireAccessTokenCookie(HttpServletResponse response) {
        log.info("{}.expireAccessTokenCookie Start!", this.getClass().getName());

        ResponseCookie cookie = ResponseCookie.from(accessCookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("{}.expireAccessTokenCookie End!", this.getClass().getName());
    }

    private void setRefreshTokenCookie(String refreshToken, Instant expiresAt, HttpServletResponse response) {
        log.info("{}.setRefreshTokenCookie Start!", this.getClass().getName());

        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, refreshToken)
                .httpOnly(true) // 항상 true (XSS 방어)
                .secure(cookieSecure) // yml 설정값 사용
                .path("/")
                .sameSite(sameSite) // yml 설정값 사용
                .maxAge(Math.max(0, Duration.between(Instant.now(), expiresAt).toSeconds()))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("{}.setRefreshTokenCookie End!", this.getClass().getName());
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        log.info("{}.extractRefreshTokenFromCookie Start!", this.getClass().getName());

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            log.info("{}.extractRefreshTokenFromCookie End!", this.getClass().getName());
            return null;
        }
        for (Cookie cookie : cookies) {
            if (refreshCookieName.equals(cookie.getName())) {
                log.info("{}.extractRefreshTokenFromCookie End!", this.getClass().getName());
                return cookie.getValue();
            }
        }

        log.info("{}.extractRefreshTokenFromCookie End!", this.getClass().getName());
        return null;
    }

    private void expireRefreshTokenCookie(HttpServletResponse response) {
        log.info("{}.expireRefreshTokenCookie Start!", this.getClass().getName());

        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, "")
                .httpOnly(true) // 항상 true (XSS 방어)
                .secure(cookieSecure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        log.info("{}.expireRefreshTokenCookie End!", this.getClass().getName());
    }
}
