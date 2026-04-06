package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.dto.response.LoginResponseDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IUserLoginService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Slf4j
@SpringBootTest
@ActiveProfiles("local")
public class UserLoginServiceTest {

    @Autowired
    IUserLoginService userLoginService;

    @Autowired
    IJwtTokenService jwtTokenService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    // DB에 실제 존재하는 계정
    private static final String EXIST_LOGIN_ID = "test";
    private static final String EXIST_PASSWORD = "test1234!";
    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";

    @Test
    @DisplayName("로그인 성공 - Access Token 반환 및 RT Cookie 설정")
    void login_success() throws Exception {
        // given
        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(EXIST_LOGIN_ID)
                .password(EXIST_PASSWORD)
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        LoginResponseDTO rDTO = userLoginService.login(pDTO, response);

        log.info("accessToken: {}", rDTO.accessToken());
        log.info("tokenType: {}", rDTO.tokenType());
        log.info("expiresIn: {}", rDTO.expiresIn());

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.accessToken()).isNotNull();
        assertThat(rDTO.tokenType()).isEqualTo("Bearer");
        assertThat(rDTO.expiresIn()).isPositive();
        assertThat(response.getCookie("RefreshToken")).isNotNull();
        assertThat(response.getCookie("RefreshToken").isHttpOnly()).isTrue();

        // cleanup: AT에서 실제 userId 추출 후 Redis 정리
        Long userId = jwtTokenService.getUserIdFromToken(rDTO.accessToken());
        log.info("cleanup - userId: {}", userId);
        stringRedisTemplate.delete(REFRESH_TOKEN_KEY_PREFIX + userId);
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 loginId")
    void login_fail_userNotFound() {
        // given
        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId("not_exist_id_xyz")
                .password("Test1234!")
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
    @DisplayName("로그인 실패 - 비밀번호 불일치")
    void login_fail_wrongPassword() {
        // given
        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(EXIST_LOGIN_ID)
                .password("WrongPassword!")
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when & then
        assertThatThrownBy(() -> userLoginService.login(pDTO, response))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.LOGIN_FAIL);
                });

        // cleanup: 실패 횟수 초기화
        stringRedisTemplate.delete("login:fail:" + EXIST_LOGIN_ID);
    }

    @Test
    @DisplayName("로그인 실패 - 계정 잠금 (5회 실패)")
    void login_fail_accountLocked() {
        // given: 강제로 계정 잠금 상태 만들기
        stringRedisTemplate.opsForValue()
                .set("login:lock:" + EXIST_LOGIN_ID, "locked");

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(EXIST_LOGIN_ID)
                .password(EXIST_PASSWORD)
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
        stringRedisTemplate.delete("login:lock:" + EXIST_LOGIN_ID);
    }

    @Test
    @DisplayName("로그아웃 - RT 삭제 및 Cookie 만료")
    void logout_success() throws Exception {
        // given: 로그인해서 RT 저장
        LoginRequestDTO loginDTO = LoginRequestDTO.builder()
                .loginId(EXIST_LOGIN_ID)
                .password(EXIST_PASSWORD)
                .build();
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();
        LoginResponseDTO loginResult = userLoginService.login(loginDTO, loginResponse);

        // AT에서 실제 userId 추출
        Long userId = jwtTokenService.getUserIdFromToken(loginResult.accessToken());
        log.info("로그아웃 대상 userId: {}", userId);

        // when
        MockHttpServletResponse logoutResponse = new MockHttpServletResponse();
        userLoginService.logout(userId, logoutResponse);

        // then: Redis RT 삭제 확인
        String savedRT = stringRedisTemplate.opsForValue()
                .get(REFRESH_TOKEN_KEY_PREFIX + userId);
        assertThat(savedRT).isNull();

        // then: Cookie 만료 확인
        assertThat(logoutResponse.getCookie("RefreshToken")).isNotNull();
        assertThat(logoutResponse.getCookie("RefreshToken").getMaxAge()).isZero();
    }
}
