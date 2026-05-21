package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.interviewservice.client.AiFeignClient;
import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.feign.AiQuestionItemDTO;
import com.algotalk.interviewservice.dto.request.CategoryItemRequestDTO;
import com.algotalk.interviewservice.dto.response.AiQuestionResponseDTO;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;
import com.algotalk.interviewservice.service.IInterviewSessionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

import static com.algotalk.interviewservice.exception.InterviewErrorCode.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class InterviewSessionServiceTest {

    @Autowired
    private IInterviewSessionService interviewSessionService;

    @MockBean
    private AiFeignClient aiFeignClient;

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

    @Test
    @Transactional
    @DisplayName("세션 생성 성공 - 직무 공통만 선택, 질문 3개")
    void createSession_success_commonOnly() throws Exception {
        // given
        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .sessionTitle("CS 모의면접 1회차")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(10L)
                                .categoryType("COMMON_CS")
                                .build()  // 자료구조/알고리즘
                ))
                .questionCount(3)
                .build();

        when(aiFeignClient.generateQuestions(any()))
                .thenReturn(mockAiResponse(3));

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);
        log.info("세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.sessionTitle()).isEqualTo("CS 모의면접 1회차");
        assertThat(rDTO.status()).isEqualTo("READY");
        assertThat(rDTO.totalQuestions()).isEqualTo(3);
        assertThat(rDTO.questions()).hasSize(3);

        assertThat(rDTO.questions()).allSatisfy(q -> {
            assertThat(q.difficulty()).isIn("EASY", "MEDIUM", "HARD");
            assertThat(q.questionIntent()).isNotBlank();
            assertThat(q.questionKeywords()).isNotEmpty();
        });
    }

    @Test
    @Transactional
    @DisplayName("세션 생성 성공 - 직무 특화만 선택, 질문 3개")
    void createSession_success_jobOnly() throws Exception {
        // given
        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .sessionTitle("백엔드 모의면접 1회차")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()  // 백엔드
                ))
                .questionCount(3)
                .build();

        when(aiFeignClient.generateQuestions(any()))
                .thenReturn(mockAiResponse(3));

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);
        log.info("세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.sessionTitle()).isEqualTo("백엔드 모의면접 1회차");
        assertThat(rDTO.status()).isEqualTo("READY");
        assertThat(rDTO.totalQuestions()).isEqualTo(3);
        assertThat(rDTO.questions()).hasSize(3);

        assertThat(rDTO.questions()).allSatisfy(q -> {
            assertThat(q.difficulty()).isIn("EASY", "MEDIUM", "HARD");
            assertThat(q.questionIntent()).isNotBlank();
            assertThat(q.questionKeywords()).isNotEmpty();
        });
    }

    @Test
    @Transactional
    @DisplayName("세션 생성 성공 - 직무 공통 1개 + 직무 특화 1개, 질문 3개")
    void createSession_success_commonAndJob() throws Exception {
        // given
        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .sessionTitle("백엔드 모의면접 2회차")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(10L)
                                .categoryType("COMMON_CS")
                                .build(),  // 자료구조/알고리즘
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()   // 백엔드
                ))
                .questionCount(3)
                .build();

        when(aiFeignClient.generateQuestions(any()))
                .thenReturn(mockAiResponse(3));

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);
        log.info("세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.sessionTitle()).isEqualTo("백엔드 모의면접 2회차");
        assertThat(rDTO.status()).isEqualTo("READY");
        assertThat(rDTO.totalQuestions()).isEqualTo(3);
        assertThat(rDTO.questions()).hasSize(3);

        assertThat(rDTO.questions()).allSatisfy(q -> {
            assertThat(q.difficulty()).isIn("EASY", "MEDIUM", "HARD");
            assertThat(q.questionIntent()).isNotBlank();
            assertThat(q.questionKeywords()).isNotEmpty();
        });
    }

    @Test
    @Transactional
    @DisplayName("세션 생성 성공 - 직무 특화 2개 + 직무 공통 1개, 질문 5개 (최대)")
    void createSession_success_maxCategories() throws Exception {
        // given
        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .sessionTitle("풀스택 모의면접 1회차")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(10L)
                                .categoryType("COMMON_CS")
                                .build(),  // 자료구조/알고리즘
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build(),  // 백엔드
                        CategoryItemRequestDTO.builder()
                                .categoryId(102L)
                                .categoryType("JOB")
                                .build()   // 풀스택
                ))
                .questionCount(5)
                .build();

        when(aiFeignClient.generateQuestions(any()))
                .thenReturn(mockAiResponse(5));

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);
        log.info("세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.totalQuestions()).isEqualTo(5);
        assertThat(rDTO.questions()).hasSize(5);

        assertThat(rDTO.questions()).allSatisfy(q -> {
            assertThat(q.difficulty()).isIn("EASY", "MEDIUM", "HARD");
            assertThat(q.questionIntent()).isNotBlank();
            assertThat(q.questionKeywords()).isNotEmpty();
        });
    }

    @Test
    @Transactional
    @DisplayName("세션 생성 성공 - 질문 1개 (최소)")
    void createSession_success_minQuestionCount() throws Exception {
        // given
        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .sessionTitle("AI 모의면접 1회차")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(110L)
                                .categoryType("JOB")
                                .build()  // AI/머신러닝
                ))
                .questionCount(1)
                .build();

        when(aiFeignClient.generateQuestions(any()))
                .thenReturn(mockAiResponse(1));

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);
        log.info("세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.questions()).hasSize(1);

        assertThat(rDTO.questions()).allSatisfy(q -> {
            assertThat(q.difficulty()).isIn("EASY", "MEDIUM", "HARD");
            assertThat(q.questionIntent()).isNotBlank();
            assertThat(q.questionKeywords()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("세션 생성 실패 - 유효하지 않은 categoryType")
    void createSession_fail_invalidCategoryType() {
        // given
        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .sessionTitle("모의면접")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(10L)
                                .categoryType("INVALID_TYPE")  // 잘못된 타입
                                .build(),
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(3)
                .build();

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                interviewSessionService.createSession(pCommand));
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_CATEGORY_TYPE);
    }

    @Test
    @DisplayName("세션 생성 실패 - 존재하지 않는 categoryId")
    void createSession_fail_invalidCategoryId() {
        // given
        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .sessionTitle("모의면접")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(999L)  // 존재하지 않는 categoryId
                                .categoryType("COMMON_CS")
                                .build()
                ))
                .questionCount(3)
                .build();

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                interviewSessionService.createSession(pCommand));
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_CATEGORY_ID);
    }

    @Test
    @DisplayName("세션 생성 실패 - aiService 응답 질문 수 불일치")
    void createSession_fail_aiResponseCountMismatch() {
        // given
        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .sessionTitle("모의면접")
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(3)
                .build();

        // aiService가 요청한 것보다 적은 질문 반환
        when(aiFeignClient.generateQuestions(any()))
                .thenReturn(mockAiResponse(2));

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                interviewSessionService.createSession(pCommand));
        assertThat(ex.getErrorCode()).isEqualTo(AI_CALL_FAILED);
    }
}