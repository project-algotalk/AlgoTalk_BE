package com.algotalk.userservice.controller;

import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.dto.request.SignUpRequestDTO;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IUserRegService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
public class UserLoginControllerTest {
    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    IJwtTokenService jwtTokenService;

    @Autowired
    IUserRegService userRegService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    @Transactional
    @DisplayName("로그인 성공 - 200 + AT/RT Cookie")
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

        MvcResult result = mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("AccessToken"))
                .andExpect(cookie().exists("RefreshToken"))
                .andExpect(cookie().httpOnly("AccessToken", true))
                .andExpect(cookie().httpOnly("RefreshToken", true))
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("SameSite")))
                .andReturn();

        log.info("응답 Body: {}", result.getResponse().getContentAsString());

        // cleanup
        String accessToken = Objects.requireNonNull(result.getResponse().getCookie("AccessToken")).getValue();

        long userId = jwtTokenService.getUserIdFromToken(accessToken);

        // cleanup
        stringRedisTemplate.delete("refresh:" + userId);
        stringRedisTemplate.delete("email:verified:" + email);
    }

    @Test
    @Transactional
    @DisplayName("로그인 실패 - 존재하지 않는 loginId -> 401")
    void login_fail_userNotFound() throws Exception {
        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId("not_exist_id_xyz")
                .password("Test1234!")
                .build();

        mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_004"));
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

        mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_001"));

        // cleanup
        stringRedisTemplate.delete("login:fail:" + loginId);
        stringRedisTemplate.delete("email:verified:" + email);
    }

    @Test
    @Transactional
    @DisplayName("로그인 실패 - 계정 잠금")
    void login_fail_accountLocked() throws Exception {
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

        // 강제로 계정 잠금 상태
        stringRedisTemplate.opsForValue()
                .set("login:lock:" + loginId, "locked");

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(loginId)
                .password(password)
                .build();

        mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_002"));

        // cleanup
        stringRedisTemplate.delete("login:lock:" + loginId);
        stringRedisTemplate.delete("email:verified:" + email);
    }

    @Test
    @Transactional
    @DisplayName("로그인 실패 - @Valid 검증 오류 (loginId 공백)")
    void login_fail_validation() throws Exception {
        // given
        String loginId = "";
        String password = "test1234!";

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(loginId)
                .password(password)
                .build();

        mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALID_001"));
    }

    @Test
    @Transactional
    @DisplayName("로그아웃 성공 - RefreshToken Cookie 만료 + Redis 삭제")
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

        MvcResult loginResult = mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isOk())
                .andReturn();

        // AT 추출
        String accessToken = Objects.requireNonNull(loginResult.getResponse().getCookie("AccessToken")).getValue();

        // when: 로그아웃 (AT를 Authorization 헤더에 포함)
        mockMvc.perform(post("/user/v1/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("Max-Age=0")));

        // cleanup
        stringRedisTemplate.delete("login:lock:" + loginId);
        stringRedisTemplate.delete("email:verified:" + email);
    }
}
