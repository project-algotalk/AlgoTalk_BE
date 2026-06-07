package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.domain.enums.RefreshTokenRotationResult;
import com.algotalk.userservice.dto.auth.RefreshTokenIssue;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.response.TokenReissueResponseDTO;
import com.algotalk.userservice.repository.IUserLoginMapper;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IRefreshTokenService;
import jakarta.servlet.http.Cookie;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static com.algotalk.userservice.exception.UserErrorCode.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
class TokenReissueServiceMockTest {

    @InjectMocks
    TokenReissueService tokenReissueService;
    @Mock IJwtTokenService jwtTokenService;
    @Mock IRefreshTokenService refreshTokenService;
    @Mock IUserLoginMapper userLoginMapper;

    private final Instant absoluteExpiresAt = Instant.now().plusSeconds(3600);
    private final Instant tokenExpiresAt = Instant.now().plusSeconds(600);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenReissueService, "accessCookieName", "AccessToken");
        ReflectionTestUtils.setField(tokenReissueService, "refreshCookieName", "RefreshToken");
        ReflectionTestUtils.setField(tokenReissueService, "cookieSecure", false);
        ReflectionTestUtils.setField(tokenReissueService, "sameSite", "Lax");
        ReflectionTestUtils.setField(tokenReissueService, "accessTokenExpiration", 600000L);
    }

    @Test
    @DisplayName("토큰 재발급 성공 - 동일 세션 유지 및 해당 세션만 RTR")
    void reissuePreservesSessionAndRotatesOnlyThatSession() throws Exception {
        String refreshToken = "valid.refresh.token";
        MockHttpServletRequest request = requestWith(refreshToken);
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserInfoCommand user = UserInfoCommand.builder().userId(1L).role("ROLE_USER").build();
        givenValidSession(refreshToken);
        given(refreshTokenService.getRefreshToken(1L, "session-a")).willReturn(refreshToken);
        given(userLoginMapper.getUserAuthInfo(any())).willReturn(user);
        given(jwtTokenService.generateAccessToken(any())).willReturn("new.access.token");
        given(jwtTokenService.rotateRefreshToken(user, "session-a", absoluteExpiresAt))
                .willReturn(new RefreshTokenIssue(
                        "new.refresh.token", "session-a", tokenExpiresAt, absoluteExpiresAt));
        given(refreshTokenService.rotateRefreshToken(
                1L, "session-a", refreshToken, "new.refresh.token", tokenExpiresAt
        )).willReturn(RefreshTokenRotationResult.ROTATED);

        TokenReissueResponseDTO result = tokenReissueService.reissueToken(request, response);

        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(600L);
        verify(refreshTokenService).rotateRefreshToken(
                1L, "session-a", refreshToken, "new.refresh.token", tokenExpiresAt);
        assertThat(response.getHeaders("Set-Cookie").toString())
                .contains("RefreshToken=new.refresh.token", "HttpOnly", "SameSite=Lax");
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Refresh Token 쿠키 없음")
    void reissueFailsWithoutCookie() {
        assertThatThrownBy(() -> tokenReissueService.reissueToken(
                new MockHttpServletRequest(), new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(REFRESH_TOKEN_NOT_FOUND));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 세션에 저장된 Refresh Token 불일치")
    void reissueFailsWhenSessionTokenMismatches() throws Exception {
        String refreshToken = "invalid.token";
        givenValidSession(refreshToken);
        given(refreshTokenService.getRefreshToken(1L, "session-a")).willReturn("different.token");

        assertThatThrownBy(() -> tokenReissueService.reissueToken(
                requestWith(refreshToken), new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(TOKEN_MISMATCH));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - Redis 세션 없음")
    void reissueFailsWhenSessionIsMissing() throws Exception {
        String refreshToken = "expired.token";
        givenValidSession(refreshToken);
        given(refreshTokenService.getRefreshToken(1L, "session-a")).willReturn(null);

        assertThatThrownBy(() -> tokenReissueService.reissueToken(
                requestWith(refreshToken), new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 다른 요청이 먼저 RTR 완료")
    void reissueFailsAfterCompetingRotationWins() throws Exception {
        String refreshToken = "valid.refresh.token";
        UserInfoCommand user = UserInfoCommand.builder().userId(1L).build();
        givenValidSession(refreshToken);
        given(refreshTokenService.getRefreshToken(1L, "session-a")).willReturn(refreshToken);
        given(userLoginMapper.getUserAuthInfo(any())).willReturn(user);
        given(jwtTokenService.generateAccessToken(any())).willReturn("new.access.token");
        given(jwtTokenService.rotateRefreshToken(user, "session-a", absoluteExpiresAt))
                .willReturn(new RefreshTokenIssue(
                        "new.refresh.token", "session-a", tokenExpiresAt, absoluteExpiresAt));
        given(refreshTokenService.rotateRefreshToken(
                1L, "session-a", refreshToken, "new.refresh.token", tokenExpiresAt
        )).willReturn(RefreshTokenRotationResult.MISMATCH);

        assertThatThrownBy(() -> tokenReissueService.reissueToken(
                requestWith(refreshToken), new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(TOKEN_MISMATCH));
    }

    @Test
    @DisplayName("토큰 재발급 실패 - 세션 절대 만료 시간 경과")
    void reissueRejectsExpiredAbsoluteSession() throws Exception {
        String refreshToken = "absolute.expired.token";
        given(jwtTokenService.getUserIdFromToken(refreshToken)).willReturn(1L);
        given(jwtTokenService.getSessionIdFromToken(refreshToken)).willReturn("session-a");
        given(jwtTokenService.getSessionExpiresAtFromToken(refreshToken))
                .willReturn(Instant.now().minusSeconds(1));

        assertThatThrownBy(() -> tokenReissueService.reissueToken(
                requestWith(refreshToken), new MockHttpServletResponse()))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(TOKEN_INVALID));
    }

    private void givenValidSession(String refreshToken) throws Exception {
        given(jwtTokenService.getUserIdFromToken(refreshToken)).willReturn(1L);
        given(jwtTokenService.getSessionIdFromToken(refreshToken)).willReturn("session-a");
        given(jwtTokenService.getSessionExpiresAtFromToken(refreshToken)).willReturn(absoluteExpiresAt);
    }

    private MockHttpServletRequest requestWith(String refreshToken) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("RefreshToken", refreshToken));
        return request;
    }
}