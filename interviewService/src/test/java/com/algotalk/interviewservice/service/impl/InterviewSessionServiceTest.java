package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.common.response.ApiResponse;
import com.algotalk.interviewservice.client.AiFeignClient;
import com.algotalk.interviewservice.client.UserFeignClient;
import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.response.AiQuestionItemDTO;
import com.algotalk.interviewservice.dto.response.CsValidationItemDTO;
import com.algotalk.interviewservice.dto.request.CategoryItemRequestDTO;
import com.algotalk.interviewservice.dto.request.ManualQuestionItemRequestDTO;
import com.algotalk.interviewservice.dto.response.AiQuestionResponseDTO;
import com.algotalk.interviewservice.dto.response.CsCategoryResponseDTO;
import com.algotalk.interviewservice.dto.response.CsValidationResponseDTO;
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

    @MockBean
    private UserFeignClient userFeignClient;

    // aiService LLM 질문 생성 Mock 응답 생성 헬퍼
    private AiQuestionResponseDTO mockAiResponse(int questionCount) {
        List<AiQuestionItemDTO> questions = IntStream.rangeClosed(1, questionCount)
                .mapToObj(i -> AiQuestionItemDTO.builder()
                        .order(i)
                        .category("자료구조/알고리즘")
                        .difficulty("MEDIUM")
                        .content("테스트 질문 " + i + "번입니다.")
                        .intent("테스트 출제 의도")
                        .keywords(List.of("키워드1", "키워드2"))
                        .build())
                .toList();
        return AiQuestionResponseDTO.builder()
                .questions(questions)
                .build();
    }

    // CS_CATEGORY Mock 단건 생성 헬퍼
    private CsCategoryResponseDTO mockCategory(Long categoryId, String categoryType, String categoryName) {
        return CsCategoryResponseDTO.builder()
                .categoryId(categoryId)
                .categoryType(categoryType)
                .categoryName(categoryName)
                .parentId(null)
                .depth(2)
                .sortOrder(1)
                .build();
    }

    // userService CS_CATEGORY 전체 목록 Mock 응답 생성 헬퍼
    private ApiResponse<List<CsCategoryResponseDTO>> mockCategoryList() {
        return ApiResponse.ok(List.of(
                mockCategory(10L, "COMMON_CS", "자료구조/알고리즘"),
                mockCategory(11L, "COMMON_CS", "데이터베이스"),
                mockCategory(12L, "COMMON_CS", "운영체제"),
                mockCategory(13L, "COMMON_CS", "네트워크"),
                mockCategory(101L, "JOB", "백엔드 개발자"),
                mockCategory(102L, "JOB", "풀스택 개발자"),
                mockCategory(110L, "JOB", "AI/머신러닝 엔지니어")
        ));
    }

    // aiService CS 질문 검증 Mock 응답 생성 헬퍼 (단건)
    private CsValidationResponseDTO mockValidationResponse(String questionText, boolean isValid) {
        return CsValidationResponseDTO.builder()
                .results(List.of(
                        CsValidationItemDTO.builder()
                                .questionText(questionText)
                                .isValid(isValid)
                                .reason(isValid ? "CS 관련 질문입니다." : "CS와 무관한 질문입니다.")
                                .build()
                ))
                .build();
    }

    @Test
    @Transactional
    @DisplayName("세션 생성 성공 - 직무 공통만 선택, 질문 3개")
    void createSession_success_commonOnly() throws Exception {
        // given
        int questionCount = 3;

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(10L)
                                .categoryType("COMMON_CS")
                                .build()
                ))
                .questionCount(questionCount)
                .build();

        when(userFeignClient.getCsCategories()).thenReturn(mockCategoryList());
        when(aiFeignClient.generateQuestions(any())).thenReturn(mockAiResponse(questionCount));

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);
        log.info("세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.sessionTitle()).contains("모의면접");
        assertThat(rDTO.status()).isEqualTo("READY");
        assertThat(rDTO.totalQuestions()).isEqualTo(questionCount);
        assertThat(rDTO.questions()).hasSize(questionCount);
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
        int questionCount = 3;

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(questionCount)
                .build();

        when(userFeignClient.getCsCategories()).thenReturn(mockCategoryList());
        when(aiFeignClient.generateQuestions(any())).thenReturn(mockAiResponse(questionCount));

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);
        log.info("세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.sessionTitle()).contains("모의면접");
        assertThat(rDTO.status()).isEqualTo("READY");
        assertThat(rDTO.totalQuestions()).isEqualTo(questionCount);
        assertThat(rDTO.questions()).hasSize(questionCount);
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
        int questionCount = 3;

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
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
                .questionCount(questionCount)
                .build();

        when(userFeignClient.getCsCategories()).thenReturn(mockCategoryList());
        when(aiFeignClient.generateQuestions(any())).thenReturn(mockAiResponse(questionCount));

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);
        log.info("세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.sessionTitle()).contains("모의면접");
        assertThat(rDTO.status()).isEqualTo("READY");
        assertThat(rDTO.totalQuestions()).isEqualTo(questionCount);
        assertThat(rDTO.questions()).hasSize(questionCount);
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
        int questionCount = 5;

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
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
                .questionCount(questionCount)
                .build();

        when(userFeignClient.getCsCategories()).thenReturn(mockCategoryList());
        when(aiFeignClient.generateQuestions(any())).thenReturn(mockAiResponse(questionCount));

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);
        log.info("세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.sessionTitle()).contains("모의면접");
        assertThat(rDTO.totalQuestions()).isEqualTo(questionCount);
        assertThat(rDTO.questions()).hasSize(questionCount);
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
        int questionCount = 1;

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(110L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(questionCount)
                .build();

        when(userFeignClient.getCsCategories()).thenReturn(mockCategoryList());
        when(aiFeignClient.generateQuestions(any())).thenReturn(mockAiResponse(questionCount));

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);
        log.info("세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.questions()).hasSize(questionCount);
        assertThat(rDTO.questions()).allSatisfy(q -> {
            assertThat(q.difficulty()).isIn("EASY", "MEDIUM", "HARD");
            assertThat(q.questionIntent()).isNotBlank();
            assertThat(q.questionKeywords()).isNotEmpty();
        });
    }

    @Test
    @DisplayName("세션 생성 실패 - 카테고리 목록 null")
    void createSession_fail_nullCategories() {
        // given
        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .selectedCategories(null)
                .questionCount(3)
                .build();

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                interviewSessionService.createSession(pCommand));
        assertThat(ex.getErrorCode()).isEqualTo(CATEGORY_REQUIRED);
    }

    @Test
    @DisplayName("세션 생성 실패 - 유효하지 않은 categoryType")
    void createSession_fail_invalidCategoryType() {
        // given
        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
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
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(999L)  // 존재하지 않는 categoryId
                                .categoryType("COMMON_CS")
                                .build()
                ))
                .questionCount(3)
                .build();

        when(userFeignClient.getCsCategories()).thenReturn(mockCategoryList());

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                interviewSessionService.createSession(pCommand));
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_CATEGORY_ID);
    }

    @Test
    @DisplayName("세션 생성 실패 - aiService 응답 질문 수 불일치")
    void createSession_fail_aiResponseCountMismatch() {
        // given
        int requestCount = 3;
        int responseCount = 2; // 요청보다 적은 수 반환

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .questionCount(requestCount)
                .build();

        when(userFeignClient.getCsCategories()).thenReturn(mockCategoryList());
        when(aiFeignClient.generateQuestions(any())).thenReturn(mockAiResponse(responseCount));

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                interviewSessionService.createSession(pCommand));
        assertThat(ex.getErrorCode()).isEqualTo(AI_CALL_FAILED);
    }

    @Test
    @Transactional
    @DisplayName("직접입력 세션 생성 성공 - 질문 2개")
    void createManualSession_success() throws Exception {
        // given
        String question1 = "프로세스와 스레드의 차이를 설명하세요.";
        String question2 = "TCP와 UDP의 차이점은 무엇인가요?";

        when(userFeignClient.getCsCategories()).thenReturn(mockCategoryList());
        when(aiFeignClient.validateCsQuestions(any()))
                .thenReturn(CsValidationResponseDTO.builder()
                        .results(List.of(
                                CsValidationItemDTO.builder()
                                        .questionText(question1)
                                        .isValid(true)
                                        .reason("CS 관련 질문입니다.")
                                        .build(),
                                CsValidationItemDTO.builder()
                                        .questionText(question2)
                                        .isValid(true)
                                        .reason("CS 관련 질문입니다.")
                                        .build()
                        ))
                        .build());

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .manualQuestions(List.of(
                        ManualQuestionItemRequestDTO.builder()
                                .categoryId(101L)
                                .questionText(question1)
                                .build(),
                        ManualQuestionItemRequestDTO.builder()
                                .categoryId(12L)
                                .questionText(question2)
                                .build()
                ))
                .build();

        // when
        SessionCreateResponseDTO rDTO = interviewSessionService.createManualSession(pCommand);
        log.info("직접입력 세션 생성 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.sessionTitle()).contains("모의면접");
        assertThat(rDTO.status()).isEqualTo("READY");
        assertThat(rDTO.totalQuestions()).isEqualTo(2);
        assertThat(rDTO.questions()).hasSize(2);
        assertThat(rDTO.questions()).allSatisfy(q -> {
            assertThat(q.sourceType()).isEqualTo("USER_INPUT");
            assertThat(q.difficulty()).isNull();
            assertThat(q.questionIntent()).isNull();
            assertThat(q.questionKeywords()).isNull();
        });
    }

    @Test
    @DisplayName("직접입력 세션 생성 실패 - 존재하지 않는 categoryId")
    void createManualSession_fail_invalidCategoryId() {
        // given
        when(userFeignClient.getCsCategories()).thenReturn(mockCategoryList());

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .manualQuestions(List.of(
                        ManualQuestionItemRequestDTO.builder()
                                .categoryId(999L)  // 존재하지 않는 categoryId
                                .questionText("테스트 질문입니다.")
                                .build()
                ))
                .build();

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                interviewSessionService.createManualSession(pCommand));
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_CATEGORY_ID);
    }

    @Test
    @DisplayName("직접입력 세션 생성 실패 - CS 관련 아닌 질문 포함")
    void createManualSession_fail_invalidCsQuestion() {
        // given
        String invalidQuestion = "오늘 점심 뭐 먹을까?";

        when(userFeignClient.getCsCategories()).thenReturn(mockCategoryList());
        when(aiFeignClient.validateCsQuestions(any()))
                .thenReturn(mockValidationResponse(invalidQuestion, false));

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .manualQuestions(List.of(
                        ManualQuestionItemRequestDTO.builder()
                                .categoryId(101L)
                                .questionText(invalidQuestion)
                                .build()
                ))
                .build();

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                interviewSessionService.createManualSession(pCommand));
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_CS_QUESTION);
    }
}