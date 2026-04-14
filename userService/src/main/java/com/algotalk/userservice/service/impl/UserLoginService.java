package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.dto.response.LoginResponseDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IUserLoginMapper;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IRefreshTokenService;
import com.algotalk.userservice.service.IUserLoginService;
import com.algotalk.userservice.util.CmmUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    @Value("${cookie.refresh.name}")
    private String refreshCookieName;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String sameSite;

    @Value("${jwt.refresh.token.expiration}")
    private long refreshTokenExpiration;

    @Override
    public LoginResponseDTO login(LoginRequestDTO pDTO, HttpServletResponse response) throws Exception {
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

        // 7. JWT Access Token 및 Refresh Token 생성
        String accessToken = jwtTokenService.generateAccessToken(rCommand);
        String refreshToken = jwtTokenService.generateRefreshToken(rCommand);

        // 8. Refresh Token Redis 저장
        refreshTokenService.saveRefreshToken(rCommand.getUserId(), refreshToken);

        // 9. Refresh Token 쿠키 설정
        setRefreshTokenCookie(refreshToken, response);

        LoginResponseDTO rDTO = LoginResponseDTO.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiration / 1000) // ms -> 초 변환
                .build();


        log.info("{}.login End!", this.getClass().getName());
        return rDTO;
    }

    @Override
    public void logout(Long userId, HttpServletResponse response) throws Exception {
        log.info("{}.logout Start!", this.getClass().getName());

        // 1. Refresh Token Redis 삭제
        refreshTokenService.deleteRefreshToken(userId);

        // 2. Refresh Token 쿠키 삭제
        expireRefreshTokenCookie(response);

        log.info("{}.logout End!", this.getClass().getName());

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

    private void setRefreshTokenCookie(String refreshToken, HttpServletResponse response) {
        log.info("{}.setRefreshTokenCookie Start!", this.getClass().getName());

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
