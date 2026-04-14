package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
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

    @Mock
    IJwtTokenService jwtTokenService;
    @Mock
    IRefreshTokenService refreshTokenService;
    @Mock
    IUserLoginMapper userLoginMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenReissueService, "refreshCookieName", "RefreshToken");
        ReflectionTestUtils.setField(tokenReissueService, "cookieSecure", false);
        ReflectionTestUtils.setField(tokenReissueService, "sameSite", "Lax");
        ReflectionTestUtils.setField(tokenReissueService, "accessTokenExpiration", 600000L);
        ReflectionTestUtils.setField(tokenReissueService, "refreshTokenExpiration", 604800000L);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() throws Exception {
        // given
        String refreshToken = "valid.refresh.token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("RefreshToken", refreshToken));

        MockHttpServletResponse response = new MockHttpServletResponse();

        UserInfoCommand user = UserInfoCommand.builder()
                .userId(1L)
                .loginId("test")
                .nickname("테스터")
                .role("ROLE_USER")
                .build();

        given(jwtTokenService.getUserIdFromToken(refreshToken)).willReturn(1L);
        given(refreshTokenService.getRefreshToken(1L)).willReturn(refreshToken);
        given(userLoginMapper.getUserAuthInfo(any())).willReturn(user);
        given(jwtTokenService.generateAccessToken(any())).willReturn("new.access.token");
        given(jwtTokenService.generateRefreshToken(any())).willReturn("new.refresh.token");

        // when
        TokenReissueResponseDTO result =
                tokenReissueService.reissueToken(request, response);

        // then
        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("new.access.token");
        assertThat(result.tokenType()).isEqualTo("Bearer");
        assertThat(result.expiresIn()).isEqualTo(600L);

        verify(refreshTokenService).rotateRefreshToken(1L, "new.refresh.token");

        String setCookie = response.getHeader("Set-Cookie");

        assertThat(setCookie).isNotNull();
        assertThat(setCookie).contains("RefreshToken=new.refresh.token");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("RefreshToken 없음")
    void reissue_fail_noCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() ->
                tokenReissueService.reissueToken(request, response)
        ).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(REFRESH_TOKEN_NOT_FOUND));
    }

    @Test
    @DisplayName("RefreshToken 불일치")
    void reissue_fail_mismatch() throws Exception {

        String refreshToken = "invalid.token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("RefreshToken", refreshToken));

        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenService.getUserIdFromToken(refreshToken)).willReturn(1L);
        given(refreshTokenService.getRefreshToken(1L)).willReturn("different.token");

        assertThatThrownBy(() ->
                tokenReissueService.reissueToken(request, response)
        ).isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(TOKEN_MISMATCH));
    }

    @Test
    @DisplayName("RefreshToken 만료")
    public void reissue_fail_expired() throws Exception {
        // given
        String refreshToken = "expired.token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("RefreshToken", refreshToken));
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenService.getUserIdFromToken(refreshToken)).willReturn(1L);
        given(refreshTokenService.getRefreshToken(1L)).willReturn(null); // Redis에 없음

        // when & then
        assertThatThrownBy(() -> tokenReissueService.reissueToken(request, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(TOKEN_EXPIRED));
    }

    @Test
    @DisplayName("RefreshToken decode 실패")
    public void reissue_fail_decodeError() throws Exception {
        // given
        String refreshToken = "malformed.token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("RefreshToken", refreshToken));
        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenService.getUserIdFromToken(refreshToken))
                .willThrow(new RuntimeException("RefreshToken decode error"));

        // when & then
        assertThatThrownBy(() -> tokenReissueService.reissueToken(request, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(TOKEN_INVALID));
    }
}