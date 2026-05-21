package com.algotalk.interviewservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.interviewservice.client.AiFeignClient;
import com.algotalk.interviewservice.client.UserFeignClient;
import com.algotalk.interviewservice.dto.feign.AiQuestionItemDTO;
import com.algotalk.interviewservice.dto.request.CategoryItemRequestDTO;
import com.algotalk.interviewservice.dto.request.SessionCreateRequestDTO;
import com.algotalk.interviewservice.dto.response.AiQuestionResponseDTO;
import com.algotalk.interviewservice.dto.response.CsCategoryResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static com.algotalk.interviewservice.exception.InterviewErrorCode.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class InterviewSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiFeignClient aiFeignClient;

    @MockBean
    private UserFeignClient userFeignClient;

    @Autowired
    private ObjectMapper objectMapper;

    private AiQuestionResponseDTO mockAiResponse(int questionCount) {
        List<AiQuestionItemDTO> questions = IntStream.rangeClosed(1, questionCount)
                .mapToObj(i -> new AiQuestionItemDTO(
                        i,
                        "자료구조/알고리즘",
                        "MEDIUM",
                        "테스트 질문 " + i + "번입니다.",
                        "테스트 출제 의도",
                        List.of("키워드1", "키워드2")
                ))
                .toList();
        return new AiQuestionResponseDTO(questions);
    }

    private CsCategoryResponseDTO mockCategory(Long categoryId, String categoryType, String categoryName) {
        return new CsCategoryResponseDTO(categoryId, categoryType, categoryName, null, 2, 1);
    }

    @Test
    @Transactional
    @DisplayName("세션 생성 성공 - 직무 공통 1개 + 직무 특화 1개, 질문 3개")
    void createSession_success_commonAndJob() throws Exception {
        // given
        SessionCreateRequestDTO pDTO = SessionCreateRequestDTO.builder()
                .sessionTitle("백엔드 모의면접 1회차")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(10L)
                                .categoryType("COMMON_CS")
                                .build(),
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(3)
                .build();

        // userService Mock 설정
        when(userFeignClient.getCsCategories())
                .thenReturn(ApiResponse.ok(List.of(
                        mockCategory(10L, "COMMON_CS", "자료구조/알고리즘"),
                        mockCategory(101L, "JOB", "백엔드 개발자"),
                        mockCategory(102L, "JOB", "풀스택 개발자"),
                        mockCategory(110L, "JOB", "AI/머신러닝 엔지니어")
                )));

        // aiService Mock 설정
        when(aiFeignClient.generateQuestions(any()))
                .thenReturn(mockAiResponse(3));

        // when, then
        mockMvc.perform(
                        post("/interview/v1/sessions")
                                .header("X-User-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").exists())
                .andExpect(jsonPath("$.data.sessionTitle").value("백엔드 모의면접 1회차"))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.totalQuestions").value(3))
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions.length()").value(3))
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("세션 생성 성공 - 직무 특화만 선택, 질문 1개 (최소)")
    void createSession_success_jobOnly_minQuestionCount() throws Exception {
        // given
        SessionCreateRequestDTO pDTO = SessionCreateRequestDTO.builder()
                .sessionTitle("AI 모의면접 1회차")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(110L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(1)
                .build();

        // userService Mock 설정
        when(userFeignClient.getCsCategories())
                .thenReturn(ApiResponse.ok(List.of(
                        mockCategory(10L, "COMMON_CS", "자료구조/알고리즘"),
                        mockCategory(101L, "JOB", "백엔드 개발자"),
                        mockCategory(102L, "JOB", "풀스택 개발자"),
                        mockCategory(110L, "JOB", "AI/머신러닝 엔지니어")
                )));

        // aiService Mock 설정
        when(aiFeignClient.generateQuestions(any()))
                .thenReturn(mockAiResponse(1));

        // when, then
        mockMvc.perform(
                        post("/interview/v1/sessions")
                                .header("X-User-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions.length()").value(1))
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("세션 생성 성공 - 직무 특화 2개 + 직무 공통 1개, 질문 5개 (최대)")
    void createSession_success_maxCategories_maxQuestionCount() throws Exception {
        // given
        SessionCreateRequestDTO pDTO = SessionCreateRequestDTO.builder()
                .sessionTitle("풀스택 모의면접 1회차")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(10L)
                                .categoryType("COMMON_CS")
                                .build(),
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build(),
                        CategoryItemRequestDTO.builder()
                                .categoryId(102L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(5)
                .build();

        // userService Mock 설정
        when(userFeignClient.getCsCategories())
                .thenReturn(ApiResponse.ok(List.of(
                        mockCategory(10L, "COMMON_CS", "자료구조/알고리즘"),
                        mockCategory(101L, "JOB", "백엔드 개발자"),
                        mockCategory(102L, "JOB", "풀스택 개발자"),
                        mockCategory(110L, "JOB", "AI/머신러닝 엔지니어")
                )));

        // aiService Mock 설정
        when(aiFeignClient.generateQuestions(any()))
                .thenReturn(mockAiResponse(5));

        // when, then
        mockMvc.perform(
                        post("/interview/v1/sessions")
                                .header("X-User-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions.length()").value(5))
                .andDo(print());
    }

    @Test
    @DisplayName("세션 생성 실패 - X-User-Id 헤더 없음")
    void createSession_fail_noHeader() throws Exception {
        // given
        SessionCreateRequestDTO pDTO = SessionCreateRequestDTO.builder()
                .sessionTitle("모의면접")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(3)
                .build();

        // when, then
        mockMvc.perform(
                        post("/interview/v1/sessions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }

    @Test
    @DisplayName("세션 생성 실패 - 세션 제목 없음 (@NotBlank)")
    void createSession_fail_noSessionTitle() throws Exception {
        // given
        SessionCreateRequestDTO pDTO = SessionCreateRequestDTO.builder()
                .sessionTitle("")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(3)
                .build();

        // when, then
        mockMvc.perform(
                        post("/interview/v1/sessions")
                                .header("X-User-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("세션 생성 실패 - 카테고리 4개 초과 (@Size)")
    void createSession_fail_categoriesOverMax() throws Exception {
        // given
        SessionCreateRequestDTO pDTO = SessionCreateRequestDTO.builder()
                .sessionTitle("모의면접")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder().categoryId(10L).categoryType("COMMON_CS").build(),
                        CategoryItemRequestDTO.builder().categoryId(11L).categoryType("COMMON_CS").build(),
                        CategoryItemRequestDTO.builder().categoryId(101L).categoryType("JOB").build(),
                        CategoryItemRequestDTO.builder().categoryId(102L).categoryType("JOB").build()
                ))
                .questionCount(3)
                .build();

        // when, then
        mockMvc.perform(
                        post("/interview/v1/sessions")
                                .header("X-User-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("세션 생성 실패 - 질문 수 0개 (@Min)")
    void createSession_fail_questionCountUnderMin() throws Exception {
        // given
        SessionCreateRequestDTO pDTO = SessionCreateRequestDTO.builder()
                .sessionTitle("모의면접")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(0)
                .build();

        // when, then
        mockMvc.perform(
                        post("/interview/v1/sessions")
                                .header("X-User-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("세션 생성 실패 - 질문 수 6개 (@Max)")
    void createSession_fail_questionCountOverMax() throws Exception {
        // given
        SessionCreateRequestDTO pDTO = SessionCreateRequestDTO.builder()
                .sessionTitle("모의면접")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(6)
                .build();

        // when, then
        mockMvc.perform(
                        post("/interview/v1/sessions")
                                .header("X-User-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("세션 생성 실패 - 유효하지 않은 categoryType")
    void createSession_fail_invalidCategoryType() throws Exception {
        // given
        SessionCreateRequestDTO pDTO = SessionCreateRequestDTO.builder()
                .sessionTitle("모의면접")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(10L)
                                .categoryType("INVALID_TYPE")
                                .build(),
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(3)
                .build();

        // when, then
        mockMvc.perform(
                        post("/interview/v1/sessions")
                                .header("X-User-Id", "1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(INVALID_CATEGORY_TYPE.getCode()))
                .andExpect(jsonPath("$.message").exists())
                .andDo(print());
    }
}