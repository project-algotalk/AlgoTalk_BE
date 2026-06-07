package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.dto.request.SignUpRequestDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IUserLoginService;
import com.algotalk.userservice.service.IUserRegService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Slf4j
@SpringBootTest
@ActiveProfiles("local")
public class UserLoginServiceTest {

    @Autowired
    IUserLoginService userLoginService;

    @Autowired
    IUserRegService userRegService;

    @Autowired
    IJwtTokenService jwtTokenService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";

    @Test
    @Transactional
    @DisplayName("로그인 성공 - Access/Refresh Token Cookie 설정")
    void login_success() throws Exception {
        // given
        String loginId = "test" + System.currentTimeMillis();
        String password = "test1234!";
        String email = loginId + "@algotalk.com";
        String name = "테스터";

        // 이메일 인증
        stringRedisTemplate.opsForValue().set("email:verified:" + email, "Y");

        // 회원가입
        userRegService.insertUser(
                SignUpRequestDTO.builder()
                        .loginId(loginId)
                        .password(password)
                        .passwordConfirm(password)
                        .name(name)
                        .email(email)
                        .build());

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(loginId)
                .password(password)
                .build();

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        userLoginService.login(pDTO, response);

        // then
        String allSetCookie = String.join("\n", response.getHeaders("Set-Cookie"));

        assertThat(allSetCookie).isNotBlank();
        assertThat(allSetCookie).contains("AccessToken=");
        assertThat(allSetCookie).contains("RefreshToken=");
        assertThat(allSetCookie).contains("HttpOnly");
        assertThat(allSetCookie).contains("SameSite");

        String accessToken = Objects.requireNonNull(response.getCookie("AccessToken")).getValue();
        Long userId = jwtTokenService.getUserIdFromToken(accessToken);

        // cleanup: sessionId 추출 후 세션 키 삭제
        String refreshToken = Objects.requireNonNull(response.getCookie("RefreshToken")).getValue();
        String sessionId = jwtTokenService.getSessionIdFromToken(refreshToken);
        stringRedisTemplate.delete(REFRESH_TOKEN_KEY_PREFIX + userId + ":" + sessionId);
        stringRedisTemplate.delete("refresh:sessions:" + userId);
        stringRedisTemplate.delete("email:verified:" + email);
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 loginId")
    void login_fail_userNotFound() {
        // given
        String loginId = "notExistId" + System.currentTimeMillis();
        String password = "test1234!";

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(loginId)
                .password(password)
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> userLoginService.login(pDTO, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                });
    }

    @Test
    @Transactional
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrongPassword() throws Exception {
        // given
        String loginId = "test" + System.currentTimeMillis();
        String password = "test1234!";
        String email = loginId + "@algotalk.com";
        String name = "테스터";

        String wrongPassword = "WrongPassword!";

        // 이메일 인증
        stringRedisTemplate.opsForValue().set("email:verified:" + email, "Y");

        // 회원가입
        userRegService.insertUser(
                SignUpRequestDTO.builder()
                        .loginId(loginId)
                        .password(password)
                        .passwordConfirm(password)
                        .name(name)
                        .email(email)
                        .build());

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(loginId)
                .password(wrongPassword)
                .build();

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> userLoginService.login(pDTO, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.LOGIN_FAIL);
                });

        // cleanup: 실패 횟수 및 이메일 인증 상태 초기화
        stringRedisTemplate.delete("login:fail:" + loginId);
        stringRedisTemplate.delete("email:verified:" + email);
    }

    @Test
    @DisplayName("로그인 실패 - 계정 잠금 (5회 실패)")
    void login_fail_accountLocked() {
        // given
        String loginId = "test" + System.currentTimeMillis();
        String password = "test1234!";

        // 강제로 계정 잠금 상태 만들기
        stringRedisTemplate.opsForValue()
                .set("login:lock:" + loginId, "locked");

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(loginId)
                .password(password)
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> userLoginService.login(pDTO, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.ACCOUNT_LOCKED);
                });

        // cleanup
        stringRedisTemplate.delete("login:lock:" + loginId);
    }

    @Test
    @Transactional
    @DisplayName("로그아웃 - RefreshToken 삭제 및 Cookie 만료")
    void logout_success() throws Exception {
        // given
        String loginId = "test" + System.currentTimeMillis();
        String password = "test1234!";
        String email = loginId + "@algotalk.com";
        String name = "테스터";

        // 이메일 인증
        stringRedisTemplate.opsForValue().set("email:verified:" + email, "Y");

        // 회원가입
        userRegService.insertUser(
                SignUpRequestDTO.builder()
                        .loginId(loginId)
                        .password(password)
                        .passwordConfirm(password)
                        .name(name)
                        .email(email)
                        .build());

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(loginId)
                .password(password)
                .build();

        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        userLoginService.login(pDTO, loginResponse);

        // AT에서 실제 userId 추출
        String accessToken = loginResponse.getCookie("AccessToken").getValue();
        Long userId = jwtTokenService.getUserIdFromToken(accessToken);
        log.info("로그아웃 대상 userId: {}", userId);

        String refreshToken = loginResponse.getCookie("RefreshToken").getValue();
        String sessionId = jwtTokenService.getSessionIdFromToken(refreshToken);
        MockHttpServletRequest logoutRequest = new MockHttpServletRequest();
        logoutRequest.setCookies(new jakarta.servlet.http.Cookie("RefreshToken", refreshToken));

        // when
        MockHttpServletResponse logoutResponse = new MockHttpServletResponse();
        userLoginService.logout(userId, logoutRequest, logoutResponse);

        // then: 현재 세션의 Redis RT 삭제 확인
        String savedRT = stringRedisTemplate.opsForValue()
                .get(REFRESH_TOKEN_KEY_PREFIX + userId + ":" + sessionId);
        assertThat(savedRT).isNull();

        // then: Cookie 만료 확인
        String allSetCookie = String.join("\n", logoutResponse.getHeaders("Set-Cookie"));

        assertThat(allSetCookie).isNotBlank();
        assertThat(allSetCookie).contains("AccessToken=");
        assertThat(allSetCookie).contains("RefreshToken=");
        assertThat(allSetCookie).contains("Max-Age=0");

        // cleanup
        stringRedisTemplate.delete("email:verified:" + email);
    }
}
