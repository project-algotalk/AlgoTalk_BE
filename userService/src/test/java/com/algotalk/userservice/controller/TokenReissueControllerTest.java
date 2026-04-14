package com.algotalk.userservice.controller;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.response.TokenReissueResponseDTO;
import com.algotalk.userservice.service.ITokenReissueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.algotalk.userservice.exception.UserErrorCode.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TokenReissueController.class)
@AutoConfigureMockMvc(addFilters = false)
class TokenReissueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ITokenReissueService tokenReissueService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissue_success() throws Exception {

        // given
        TokenReissueResponseDTO responseDTO = TokenReissueResponseDTO.builder()
                .accessToken("new.access.token")
                .tokenType("Bearer")
                .expiresIn(600L)
                .build();

        given(tokenReissueService.reissueToken(any(), any()))
                .willReturn(responseDTO);

        // when & then
        mockMvc.perform(post("/user/v1/token/reissue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(600));
    }

    @Test
    @DisplayName("RefreshToken 없음")
    void reissue_fail_noToken() throws Exception {
        // given
        given(tokenReissueService.reissueToken(any(), any()))
                .willThrow(new BusinessException(REFRESH_TOKEN_NOT_FOUND));

        // when & then
        mockMvc.perform(post("/user/v1/token/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(REFRESH_TOKEN_NOT_FOUND.getCode()));
    }

    @Test
    @DisplayName("유효하지 않은 토큰")
    void reissue_fail_invalid() throws Exception {
        // given
        given(tokenReissueService.reissueToken(any(), any()))
                .willThrow(new BusinessException(TOKEN_INVALID));

        // when & then
        mockMvc.perform(post("/user/v1/token/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TOKEN_INVALID.getCode()));

    }

    @Test
    @DisplayName("토큰 만료")
    void reissue_fail_expired() throws Exception {
        given(tokenReissueService.reissueToken(any(), any()))
                .willThrow(new BusinessException(TOKEN_EXPIRED));

        mockMvc.perform(post("/user/v1/token/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TOKEN_EXPIRED.getCode()));
    }

    @Test
    @DisplayName("저장된 토큰과 불일치")
    void reissue_fail_mismatch() throws Exception {
        given(tokenReissueService.reissueToken(any(), any()))
                .willThrow(new BusinessException(TOKEN_MISMATCH));

        mockMvc.perform(post("/user/v1/token/reissue"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TOKEN_MISMATCH.getCode()));
    }
}