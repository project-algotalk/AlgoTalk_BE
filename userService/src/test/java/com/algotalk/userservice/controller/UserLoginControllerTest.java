package com.algotalk.userservice.controller;

import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.service.IJwtTokenService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
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
    StringRedisTemplate stringRedisTemplate;

    private static final String EXIST_LOGIN_ID = "test";
    private static final String EXIST_PASSWORD = "test1234!";

    @Test
    @DisplayName("로그인 성공 - 200 + AT Body + RT Cookie")
    void login_success() throws Exception {
        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(EXIST_LOGIN_ID)
                .password(EXIST_PASSWORD)
                .build();

        MvcResult result = mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").isNumber())
                .andExpect(cookie().exists("RefreshToken"))
                .andExpect(cookie().httpOnly("RefreshToken", true))
                .andReturn();

        log.info("응답 Body: {}", result.getResponse().getContentAsString());

        // cleanup
        // readTree: JSON을 자바 객체가 아닌 트리 구조로 읽고 특정 값을 접근할 수 있게 해줌
        String accessToken = objectMapper.readTree(
                result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        long userId = jwtTokenService.getUserIdFromToken(accessToken);

        stringRedisTemplate.delete("refresh:" + userId);
    }

    @Test
    @DisplayName("로그인 실패 - 존재하지 않는 loginId -> 401")
    void login_fail_userNotFound() throws Exception {
        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId("not_exist_id_xyz")
                .password("Test1234!")
                .build();

        mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_004"));
    }

    @Test
    @DisplayName("로그인 실패 - 비밀번호 불일치 -> 401")
    void login_fail_wrongPassword() throws Exception {
        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(EXIST_LOGIN_ID)
                .password("WrongPassword!")
                .build();

        mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_001"));

        // cleanup
        stringRedisTemplate.delete("login:fail:" + EXIST_LOGIN_ID);
    }

    @Test
    @DisplayName("로그인 실패 - 계정 잠금 -> 403")
    void login_fail_accountLocked() throws Exception {
        // given: 강제로 계정 잠금 상태
        stringRedisTemplate.opsForValue()
                .set("login:lock:" + EXIST_LOGIN_ID, "locked");

        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId(EXIST_LOGIN_ID)
                .password(EXIST_PASSWORD)
                .build();

        mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_002"));

        // cleanup
        stringRedisTemplate.delete("login:lock:" + EXIST_LOGIN_ID);
    }

    @Test
    @DisplayName("로그인 실패 - @Valid 검증 오류 (loginId 공백)")
    void login_fail_validation() throws Exception {
        LoginRequestDTO pDTO = LoginRequestDTO.builder()
                .loginId("")
                .password("Test1234!")
                .build();

        mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALID_001"));
    }

    @Test
    @DisplayName("로그아웃 성공 - RT Cookie 만료 + Redis 삭제")
    void logout_success() throws Exception {
        // given: 먼저 로그인
        LoginRequestDTO loginDTO = LoginRequestDTO.builder()
                .loginId(EXIST_LOGIN_ID)
                .password(EXIST_PASSWORD)
                .build();

        MvcResult loginResult = mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andReturn();

        // AT 추출
        String responseBody = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody)
                .path("data").path("accessToken").asText();
        log.info("로그아웃 테스트용 AT: {}", accessToken);

        // when: 로그아웃 (AT를 Authorization 헤더에 포함)
        mockMvc.perform(post("/user/v1/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("RefreshToken", 0));
    }
}
