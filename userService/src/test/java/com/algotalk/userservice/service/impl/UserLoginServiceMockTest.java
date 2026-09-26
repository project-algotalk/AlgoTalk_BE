package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.auth.CustomUserDetails;
import com.algotalk.userservice.dto.auth.RefreshTokenIssue;
import com.algotalk.userservice.dto.auth.UserAuthDTO;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IUserLoginMapper;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.ILoginAttemptService;
import com.algotalk.userservice.service.IRefreshTokenService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLoginServiceMockTest {

    @InjectMocks
    private UserLoginService userLoginService;

    @Mock
    private IUserLoginMapper userLoginMapper;

    @Mock
    private IJwtTokenService jwtTokenService;

    @Mock
    private IRefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private ILoginAttemptService loginAttemptService;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userLoginService, "accessCookieName", "AccessToken");
        ReflectionTestUtils.setField(userLoginService, "refreshCookieName", "RefreshToken");
        ReflectionTestUtils.setField(userLoginService, "cookieSecure", false);
        ReflectionTestUtils.setField(userLoginService, "sameSite", "Lax");
        ReflectionTestUtils.setField(userLoginService, "accessTokenExpiration", 600000L);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // given
        UserInfoCommand userInfo = createUserInfo();

        CustomUserDetails userDetails = new CustomUserDetails(
                new UserAuthDTO(
                        1L,
                        "testuser",
                        "encodedPassword",
                        List.of("ROLE_USER")
                )
        );

        given(loginAttemptService.isLocked("testuser")).willReturn(false);
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(userDetails);
        given(userLoginMapper.getUserAuthInfo(any())).willReturn(userInfo);
        given(jwtTokenService.issueRefreshToken(userInfo)).willReturn(
                new RefreshTokenIssue(
                        "mock.refresh.token",
                        "session-a",
                        Instant.now().plusSeconds(600),
                        Instant.now().plusSeconds(3600)
                )
        );
        given(jwtTokenService.generateAccessToken(userInfo, "session-a"))
                .willReturn("mock.access.token");

        LoginRequestDTO request = LoginRequestDTO.builder()
                .loginId("testuser")
                .password("Test1234!")
                .build();

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        userLoginService.login(request, response);

        // then
        verify(loginAttemptService).resetAttempts("testuser");
        verify(jwtTokenService).generateAccessToken(userInfo, "session-a");
        verify(refreshTokenService).saveRefreshToken(
                eq(1L),
                eq("session-a"),
                eq("mock.refresh.token"),
                any(Instant.class)
        );

        String cookies = String.join("\n", response.getHeaders("Set-Cookie"));

        assertThat(cookies).isNotBlank();
        assertThat(cookies).contains("AccessToken=");
        assertThat(cookies).contains("RefreshToken=");
        assertThat(cookies).contains("HttpOnly");
        assertThat(cookies).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("로그인 실패 - 계정 잠금")
    void login_fail_accountLocked() {
        // given
        given(loginAttemptService.isLocked("testuser")).willReturn(true);

        LoginRequestDTO request = LoginRequestDTO.builder()
                .loginId("testuser")
                .password("Test1234!")
                .build();

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when, then
        assertThatThrownBy(() -> userLoginService.login(request, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(UserErrorCode.ACCOUNT_LOCKED);
                });

        verifyNoInteractions(authenticationManager);
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void login_fail_userNotFound() {
        // given
        given(loginAttemptService.isLocked("not_exist")).willReturn(false);
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(new UsernameNotFoundException("not_exist"));

        LoginRequestDTO request = LoginRequestDTO.builder()
                .loginId("not_exist")
                .password("Test1234!")
                .build();

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when, then
        assertThatThrownBy(() -> userLoginService.login(request, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(UserErrorCode.USER_NOT_FOUND);
                });

        verify(loginAttemptService, never()).recordFailure(anyString());
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrongPassword() {
        // given
        given(loginAttemptService.isLocked("testuser")).willReturn(false);
        given(authenticationManager.authenticate(any(Authentication.class)))
                .willThrow(new BadCredentialsException("비밀번호 불일치"));

        LoginRequestDTO request = LoginRequestDTO.builder()
                .loginId("testuser")
                .password("WrongPassword!")
                .build();

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when, then
        assertThatThrownBy(() -> userLoginService.login(request, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(UserErrorCode.LOGIN_FAIL);
                });

        verify(loginAttemptService).recordFailure("testuser");
        verify(loginAttemptService, never()).resetAttempts(anyString());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("RefreshToken", "refresh-token"));

        MockHttpServletResponse response = new MockHttpServletResponse();

        given(jwtTokenService.getUserIdFromToken("refresh-token")).willReturn(1L);
        given(jwtTokenService.getSessionIdFromToken("refresh-token"))
                .willReturn("session-a");

        // when
        userLoginService.logout(1L, request, response);

        // then
        verify(refreshTokenService).deleteRefreshToken(1L, "session-a");

        String cookies = String.join("\n", response.getHeaders("Set-Cookie"));

        assertThat(cookies).isNotBlank();
        assertThat(cookies).contains("AccessToken=");
        assertThat(cookies).contains("RefreshToken=");
        assertThat(cookies).contains("HttpOnly");
        assertThat(cookies).contains("SameSite=Lax");
        assertThat(cookies).contains("Max-Age=0");
    }

    @Test
    @DisplayName("모든 기기 로그아웃 성공")
    void logoutAll_success() throws Exception {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        userLoginService.logoutAll(1L, response);

        // then
        verify(refreshTokenService).deleteAllRefreshTokens(1L);

        String cookies = String.join("\n", response.getHeaders("Set-Cookie"));
        assertThat(cookies).contains("Max-Age=0");
    }

    private UserInfoCommand createUserInfo() {
        return UserInfoCommand.builder()
                .userId(1L)
                .loginId("testuser")
                .password("encodedPassword")
                .passwordSetYn("Y")
                .nickname("테스터")
                .deletedYn("N")
                .role("ROLE_USER")
                .build();
    }
}