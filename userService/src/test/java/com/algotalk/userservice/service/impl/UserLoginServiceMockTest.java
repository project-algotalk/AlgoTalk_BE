package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.dto.response.LoginResponseDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IUserLoginMapper;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IRefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 엄격한 검증 수행하지 않도록 설정(LENIENT, STRICT_STUBS, DEFAULT 중 선택)
class UserLoginServiceMockTest {

    @InjectMocks
    UserLoginService userLoginService;

    @Mock IUserLoginMapper userLoginMapper;
    @Mock IJwtTokenService jwtTokenService;
    @Mock IRefreshTokenService refreshTokenService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock StringRedisTemplate stringRedisTemplate;
    @Mock ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        // 내부 상태 설정(내부의 private 필드에 직접 값 주입)
        ReflectionTestUtils.setField(userLoginService, "accessCookieName", "AccessToken");
        ReflectionTestUtils.setField(userLoginService, "refreshCookieName", "RefreshToken");
        ReflectionTestUtils.setField(userLoginService, "cookieSecure", false);
        ReflectionTestUtils.setField(userLoginService, "sameSite", "Lax");
        ReflectionTestUtils.setField(userLoginService, "maxFailCount", 5);
        ReflectionTestUtils.setField(userLoginService, "lockMinutes", 1L);
        ReflectionTestUtils.setField(userLoginService, "accessTokenExpiration", 600000L);
        ReflectionTestUtils.setField(userLoginService, "refreshTokenExpiration", 604800000L);
    }

    @Test
    @DisplayName("로그인 성공")
    void login_success() throws Exception {
        // given
        UserInfoCommand userInfo = UserInfoCommand.builder()
                .userId(1L)
                .loginId("testuser")
                .password("encodedPassword")
                .nickname("테스터")
                .deletedYn("N")
                .role("ROLE_USER")
                .build();

        given(stringRedisTemplate.hasKey(anyString())).willReturn(false); // 잠금 없음
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(userLoginMapper.getUserAuthInfo(any())).willReturn(userInfo);
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);
        given(jwtTokenService.generateAccessToken(any())).willReturn("mock.access.token");
        given(jwtTokenService.generateRefreshToken(any())).willReturn("mock.refresh.token");

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId("testuser")
                .password("Test1234!")
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        userLoginService.login(pDTO, response);

        // then
        verify(refreshTokenService).saveRefreshToken(anyLong(), anyString()); // RefreshToken 저장 여부 검증

        String allSetCookie = String.join("\n", response.getHeaders("Set-Cookie"));

        assertThat(allSetCookie).isNotBlank();
        assertThat(allSetCookie).contains("AccessToken=");
        assertThat(allSetCookie).contains("RefreshToken=");
        assertThat(allSetCookie).contains("HttpOnly");
        assertThat(allSetCookie).contains("SameSite=Lax");
    }

    @Test
    @DisplayName("로그인 실패 - 계정 잠금")
    void login_fail_accountLocked() {
        // given: 계정 잠금 상태
        given(stringRedisTemplate.hasKey(anyString())).willReturn(true);

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId("testuser")
                .password("Test1234!")
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> userLoginService.login(pDTO, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.ACCOUNT_LOCKED));
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 사용자")
    void login_fail_userNotFound() throws Exception {
        // given: DB 조회 결과 null
        given(stringRedisTemplate.hasKey(anyString())).willReturn(false);
        given(userLoginMapper.getUserAuthInfo(any())).willReturn(null);

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId("not_exist")
                .password("Test1234!")
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> userLoginService.login(pDTO, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.USER_NOT_FOUND));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrongPassword() throws Exception {
        // given
        UserInfoCommand userInfo = UserInfoCommand.builder()
                .userId(1L)
                .loginId("testuser")
                .password("encodedPassword")
                .deletedYn("N")
                .build();

        given(stringRedisTemplate.hasKey(anyString())).willReturn(false);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(1L);
        given(userLoginMapper.getUserAuthInfo(any())).willReturn(userInfo);
        given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId("testuser")
                .password("WrongPassword!")
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> userLoginService.login(pDTO, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(UserErrorCode.LOGIN_FAIL));
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_success() throws Exception {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        userLoginService.logout(1L, response);

        // then
        verify(refreshTokenService).deleteRefreshToken(1L);

        String allSetCookie = String.join("\n", response.getHeaders("Set-Cookie"));

        assertThat(allSetCookie).isNotBlank();
        assertThat(allSetCookie).contains("AccessToken=");
        assertThat(allSetCookie).contains("RefreshToken=");
        assertThat(allSetCookie).contains("HttpOnly");
        assertThat(allSetCookie).contains("SameSite=Lax");
        assertThat(allSetCookie).contains("Max-Age=0");
    }
}