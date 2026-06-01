package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.interviewservice.client.AiFeignClient;
import com.algotalk.interviewservice.domain.InterviewAnalysisDocument;
import com.algotalk.interviewservice.domain.enums.CsCategoryType;
import com.algotalk.interviewservice.dto.command.RecentQuestionSearchCommand;
import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.command.SessionResultCommand;
import com.algotalk.interviewservice.dto.response.*;
import com.algotalk.interviewservice.dto.request.AiQuestionRequestDTO;
import com.algotalk.interviewservice.dto.request.CategoryItemRequestDTO;
import com.algotalk.interviewservice.dto.request.CsValidationRequestDTO;
import com.algotalk.interviewservice.dto.request.ManualQuestionItemRequestDTO;
import com.algotalk.interviewservice.dto.row.SessionResultRowDTO;
import com.algotalk.interviewservice.persistence.mongodb.IInterviewAnalysisMapper;
import com.algotalk.interviewservice.repository.IInterviewSessionMapper;
import com.algotalk.interviewservice.repository.ISessionQuestionMapper;
import com.algotalk.interviewservice.service.ICsCategoryService;
import com.algotalk.interviewservice.service.IInterviewSessionService;
import com.algotalk.interviewservice.service.ISessionSaveService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.algotalk.interviewservice.exception.InterviewErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService implements IInterviewSessionService {

    private final AiFeignClient aiFeignClient;
    private final ISessionSaveService sessionSaveService;
    private final ICsCategoryService csCategoryService;
    private final ISessionQuestionMapper sessionQuestionMapper;
    private final IInterviewSessionMapper interviewSessionMapper;
    private final IInterviewAnalysisMapper interviewAnalysisMapper;

    @Override
    public SessionCreateResponseDTO createSession(SessionCreateCommand pCommand) {
        log.info("{}.createSession Start!", this.getClass().getName());

        if (pCommand.getSelectedCategories() == null || pCommand.getSelectedCategories().isEmpty()) {
            throw new BusinessException(CATEGORY_REQUIRED);
        }

        // 1. categoryType 유효성 검증 (COMMON_CS 또는 JOB만 허용)
        boolean hasInvalidType = pCommand.getSelectedCategories().stream()
                .anyMatch(c -> !CsCategoryType.isValid(c.categoryType()));

        if (hasInvalidType) {
            throw new BusinessException(INVALID_CATEGORY_TYPE);
        }

        // 2. categoryId -> 카테고리명 변환 (userService 조회 + Caffeine 캐시)
        List<String> categoryNames = pCommand.getSelectedCategories().stream()
                .map(c -> csCategoryService.getCategoryById(c.categoryId()).categoryName())
                .toList();

        String sessionTitle = String.join(" · ", categoryNames) + " 모의면접";

        // 3. 유저별 최근 출제 질문 조회 (재출제 방지)
        List<Long> categoryIds = pCommand.getSelectedCategories().stream()
                .map(CategoryItemRequestDTO::categoryId)
                .toList();

        List<String> previousQuestions = sessionQuestionMapper
                .findRecentQuestionsByUserAndCategories(
                        RecentQuestionSearchCommand.builder()
                                .userId(pCommand.getUserId())
                                .categoryIds(categoryIds)
                                .limit(30)
                                .build()
                );

        log.info("previousQuestions 조회 결과: {}개 - {}", previousQuestions.size(), previousQuestions);

        // 3. aiService 호출하여 LLM 질문 생성 (트랜잭션 밖에서 수행)
        AiQuestionRequestDTO aiRequest = AiQuestionRequestDTO.builder()
                .categories(categoryNames)
                .questionCount(pCommand.getQuestionCount())
                .previousQuestions(previousQuestions)
                .build();

        AiQuestionResponseDTO aiResponse;

        try {
            aiResponse = aiFeignClient.generateQuestions(aiRequest);
        } catch (FeignException.GatewayTimeout e) {
            log.error("[AI_CALL_FAILED][generateQuestions] 타임아웃. categories={}, questionCount={}",
                    categoryNames, pCommand.getQuestionCount(), e);
            throw new BusinessException(AI_CALL_FAILED);
        } catch (FeignException e) {
            log.error("[AI_CALL_FAILED][generateQuestions] HTTP 오류. status={}, categories={}, questionCount={}",
                    e.status(), categoryNames, pCommand.getQuestionCount(), e);
            throw new BusinessException(AI_CALL_FAILED);
        } catch (Exception e) {
            log.error("[AI_CALL_FAILED][generateQuestions] type={}, categories={}, questionCount={}, message={}",
                    e.getClass().getSimpleName(), categoryNames, pCommand.getQuestionCount(), e.getMessage(), e);
            throw new BusinessException(AI_CALL_FAILED);
        }

        // 4. aiService 응답 검증
        if (aiResponse == null || aiResponse.questions() == null || aiResponse.questions().isEmpty()) {
            log.error("[AI_CALL_FAILED][generateQuestions] 응답 payload 이상. responseNull={}, questionsNullOrEmpty={}",
                    aiResponse == null,
                    aiResponse == null || aiResponse.questions() == null || aiResponse.questions().isEmpty());
            throw new BusinessException(AI_CALL_FAILED);
        }

        if (aiResponse.questions().size() != pCommand.getQuestionCount()) {
            log.error("[AI_CALL_FAILED][generateQuestions] 질문 수 불일치. requested={}, returned={}",
                    pCommand.getQuestionCount(), aiResponse.questions().size());
            throw new BusinessException(AI_CALL_FAILED);
        }

        // 5. DB 저장 (별도 빈의 @Transactional 적용)
        SessionCreateResponseDTO rDTO = sessionSaveService.saveSession(
                pCommand.toBuilder()
                        .sessionTitle(sessionTitle)
                        .categoryNames(categoryNames)
                        .build(),
                aiResponse.questions()
        );

        log.info("{}.createSession End!", this.getClass().getName());

        return rDTO;
    }

    @Override
    public SessionCreateResponseDTO createManualSession(SessionCreateCommand pCommand) {
        log.info("{}.createManualSession Start!", this.getClass().getName());

        // 1. 질문 목록 검증
        List<ManualQuestionItemRequestDTO> manualQuestions = pCommand.getManualQuestions();
        if (manualQuestions == null || manualQuestions.isEmpty()) {
            throw new BusinessException(MANUAL_QUESTION_REQUIRED);
        }

        // 2. categoryId 실존 검증 + 카테고리명 기반 세션 제목 자동 생성
        List<String> categoryNames = manualQuestions.stream()
                .map(q -> csCategoryService.getCategoryById(q.categoryId()).categoryName())
                .distinct()
                .toList();

        String sessionTitle = String.join(" · ", categoryNames) + " 모의면접";

        // 3. CS 관련 질문 여부 검증 (aiService 호출)
        List<String> questionTexts = manualQuestions.stream()
                .map(ManualQuestionItemRequestDTO::questionText)
                .toList();

        CsValidationResponseDTO validationResponse;
        try {
            validationResponse = aiFeignClient.validateCsQuestions(
                    CsValidationRequestDTO.builder()
                            .questions(questionTexts)
                            .build()
            );
        } catch (FeignException.GatewayTimeout e) {
            log.error("[AI_CALL_FAILED][validateCsQuestions] 타임아웃. questionCount={}",
                    questionTexts.size(), e);
            throw new BusinessException(AI_CALL_FAILED);
        } catch (FeignException e) {
            log.error("[AI_CALL_FAILED][validateCsQuestions] HTTP 오류. status={}, questionCount={}",
                    e.status(), questionTexts.size(), e);
            throw new BusinessException(AI_CALL_FAILED);
        } catch (Exception e) {
            log.error("[AI_CALL_FAILED][validateCsQuestions] type={}, questionCount={}, message={}",
                    e.getClass().getSimpleName(), questionTexts.size(), e.getMessage(), e);
            throw new BusinessException(AI_CALL_FAILED);
        }

        // 4. CS 관련 아닌 질문 필터링
        boolean responseNull = validationResponse == null;
        boolean resultsNull = !responseNull && validationResponse.results() == null;

        if (responseNull || resultsNull) {
            log.error("[AI_CALL_FAILED][validateCsQuestions] 응답 payload 이상. responseNull={}, resultsNull={}",
                    responseNull, resultsNull);
            throw new BusinessException(AI_CALL_FAILED);
        }

        // results 수 불일치 검증
        if (validationResponse.results().isEmpty() ||
                validationResponse.results().size() != questionTexts.size()) {
            log.error("[AI_CALL_FAILED][validateCsQuestions] 결과 수 불일치. requested={}, returned={}",
                    questionTexts.size(), validationResponse.results().size());
            throw new BusinessException(AI_CALL_FAILED);
        }

        List<String> invalidQuestions = validationResponse.results().stream()
                .filter(r -> !r.isValid())
                .map(CsValidationItemDTO::questionText)
                .toList();

        if (!invalidQuestions.isEmpty()) {
            log.warn("[AI_CALL_FAILED][validateCsQuestions] CS 관련 아닌 질문 포함: {}", invalidQuestions);
            throw new BusinessException(INVALID_CS_QUESTION);
        }

        // 5. DB 저장 (트랜잭션 범위)
        SessionCreateResponseDTO rDTO = sessionSaveService.saveManualSession(
                pCommand.toBuilder().sessionTitle(sessionTitle).build()
        );

        log.info("{}.createManualSession End!", this.getClass().getName());

        return rDTO;
    }

    @Override
    public SessionResultResponseDTO getSessionResult(SessionResultCommand pCommand) {
        log.info("{}.getSessionResult Start!", this.getClass().getName());

        // 1. SESSION_QUESTION 기준 질문 목록 조회
        List<SessionResultRowDTO> rows = interviewSessionMapper.getSessionResult(pCommand);

        if (rows == null || rows.isEmpty()) {
            throw new BusinessException(SESSION_NOT_FOUND);
        }

        SessionResultRowDTO first = rows.get(0);

        // 2. MongoDB에서 분석 결과 조회
        List<InterviewAnalysisDocument> analysisList =
                interviewAnalysisMapper.findBySessionId(pCommand);

        // 3. sessionQuestionId 기준으로 Map 변환 (빠른 조회)
        Map<Long, InterviewAnalysisDocument> analysisMap = analysisList.stream()
                .collect(Collectors.toMap(
                        InterviewAnalysisDocument::getSessionQuestionId,
                        doc -> doc,
                        (existing, replacement) -> replacement
                ));

        // 4. 질문별 분석 결과 매핑
        List<QuestionResultDTO> questions = rows.stream()
                .map(q -> {
                    InterviewAnalysisDocument doc = analysisMap.get(q.sessionQuestionId());

                    return doc == null
                            ? QuestionResultDTO.empty(q.sessionQuestionId(), q.questionOrder(), q.questionText())
                            : QuestionResultDTO.from(doc, q.questionOrder(), q.questionText());
                })
                .toList();

        SessionResultResponseDTO rDTO = SessionResultResponseDTO.builder()
                .sessionId(first.sessionId())
                .sessionTitle(first.sessionTitle())
                .totalQuestions(first.totalQuestions())
                .questions(questions)
                .build();

        log.info("{}.getSessionResult End!", this.getClass().getName());
        return rDTO;
    }

    @Override
    public void completeSession(Long userId, Long sessionId) {
        log.info("{}.completeSession Start!", this.getClass().getName());
        interviewSessionMapper.completeSession(sessionId);
        log.info("{}.completeSession End!", this.getClass().getName());
    }
}