package com.algotalk.userservice.controller;

import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.dto.request.SignUpRequestDTO;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IRefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    IJwtTokenService jwtTokenService;
    @Autowired
    IRefreshTokenService refreshTokenService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Value("${cookie.access.name}")
    private String accessCookieName;
    @Value("${cookie.refresh.name}")
    private String refreshCookieName;

    private String getSetCookieHeader(MvcResult result, String cookieName) {
        String setCookieHeader = result.getResponse().getHeaders("Set-Cookie").stream()
                .filter(header -> header.startsWith(cookieName + "="))
                .findFirst()
                .orElseThrow(
                        () -> new AssertionError("Set-Cookie 헤더에서 쿠키를 찾을 수 없습니다.")
                );

        return setCookieHeader;
    }

    private String getSetCookieValue(MvcResult result, String cookieName) {
        String setCookieHeader = getSetCookieHeader(result, cookieName);

        String prefix = cookieName + "=";
        int startIndex = prefix.length();
        int endIndex = setCookieHeader.indexOf(";");

        if(endIndex < 0) {
            endIndex = setCookieHeader.length();
        }

        return setCookieHeader.substring(startIndex, endIndex);
    }

    @Test
    @Transactional
    @DisplayName("인증 전체 흐름 테스트 - 회원가입, 로그인, 재발급, 로그아웃")
    void auth_flow_full() throws Exception {
        // given
        String loginId = "test" + System.currentTimeMillis();
        String password = "test1234!";
        String email = loginId + "@algotalk.com";

        stringRedisTemplate.opsForValue()
                .set("email:verified:" + email, "Y");

        // 회원가입
        SignUpRequestDTO request = SignUpRequestDTO.builder()
                .loginId(loginId)
                .password(password)
                .passwordConfirm(password)
                .email(email)
                .name("테스터")
                .nickname("닉네임")
                .build();

        mockMvc.perform(post("/user/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // 로그인
        MvcResult loginResult = mockMvc.perform(post("/user/v1/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                LoginRequestDTO.builder()
                                        .loginId(loginId)
                                        .password(password)
                                        .build()
                        )))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = getSetCookieValue(loginResult, accessCookieName);

        String refreshToken = getSetCookieValue(loginResult, refreshCookieName);

        // 재발급
        MvcResult reissueResult = mockMvc.perform(post("/user/v1/token/reissue")
                        .cookie(new MockCookie(refreshCookieName, refreshToken)))
                .andExpect(status().isOk())
                .andExpect(header().string("Authorization", startsWith("Bearer ")))
                .andReturn();

        String newAccessToken = Objects.requireNonNull(reissueResult.getResponse().getHeader("Authorization")).replace("Bearer ", "");

        String newRefreshToken = getSetCookieValue(reissueResult, refreshCookieName);

        // 핵심 검증 (RTR)
        assertThat(newRefreshToken).isNotEqualTo(refreshToken);

        // 로그아웃
        MvcResult logoutResult = mockMvc.perform(post("/user/v1/logout")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk())
                .andReturn();

        String logoutSetCookieHeader = getSetCookieHeader(logoutResult, refreshCookieName);
        assertThat(logoutSetCookieHeader).contains("Max-Age=0");

        // cleanup
        stringRedisTemplate.delete("email:verified:" + email);
        Long userId = jwtTokenService.getUserIdFromToken(newAccessToken);
        refreshTokenService.deleteAllRefreshTokens(userId);
    }
}