package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.interviewservice.client.AiFeignClient;
import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.feign.CsValidationItemDTO;
import com.algotalk.interviewservice.dto.request.AiQuestionRequestDTO;
import com.algotalk.interviewservice.dto.request.CategoryItemRequestDTO;
import com.algotalk.interviewservice.dto.request.CsValidationRequestDTO;
import com.algotalk.interviewservice.dto.request.ManualQuestionItemRequestDTO;
import com.algotalk.interviewservice.dto.response.AiQuestionResponseDTO;
import com.algotalk.interviewservice.dto.response.CsValidationResponseDTO;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;
import com.algotalk.interviewservice.service.ICsCategoryService;
import com.algotalk.interviewservice.service.IInterviewSessionService;
import com.algotalk.interviewservice.service.ISessionSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.algotalk.interviewservice.exception.InterviewErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService implements IInterviewSessionService {

    private final AiFeignClient aiFeignClient;
    private final ISessionSaveService sessionSaveService;
    private final ICsCategoryService csCategoryService;

    @Override
    public SessionCreateResponseDTO createSession(SessionCreateCommand pCommand) throws Exception {
        log.info("{}.createSession Start!", this.getClass().getName());

        // 1. selectedCategories 검증
        validateCategories(pCommand.getSelectedCategories());

        // 2. categoryId -> 카테고리명 변환 (userService 조회 + Caffeine 캐시)
        List<String> categoryNames = pCommand.getSelectedCategories().stream()
                .map(c -> csCategoryService.getCategoryById(c.categoryId()).categoryName())
                .toList();

        // 세션 제목 생성 (카테고리명 기반)
        String sessionTitle = String.join(" · ", categoryNames) + " 모의면접";

        // 3. aiService 호출하여 LLM 질문 생성 (트랜잭션 밖에서 수행)
        AiQuestionRequestDTO aiRequest = AiQuestionRequestDTO.builder()
                .categories(categoryNames)
                .questionCount(pCommand.getQuestionCount())
                .build();

        AiQuestionResponseDTO aiResponse;
        try {
            aiResponse = aiFeignClient.generateQuestions(aiRequest);
        } catch (Exception e) {
            // aiService 호출 실패
            log.error("aiService 호출 실패. categories={}, questionCount={}",
                    categoryNames, pCommand.getQuestionCount(), e);
            throw new BusinessException(AI_CALL_FAILED);
        }

        // 4. aiService 응답 검증
        if (aiResponse == null || aiResponse.questions() == null || aiResponse.questions().isEmpty()) {
            throw new BusinessException(AI_CALL_FAILED);
        }
        if (aiResponse.questions().size() != pCommand.getQuestionCount()) {
            throw new BusinessException(AI_CALL_FAILED);
        }

        // 5. DB 저장 (별도 빈의 @Transactional 적용)
        SessionCreateResponseDTO rDTO = sessionSaveService.saveSession(pCommand.toBuilder().sessionTitle(sessionTitle).build(), aiResponse.questions());

        log.info("{}.createSession End!", this.getClass().getName());

        return rDTO;
    }

    @Override
    public SessionCreateResponseDTO createManualSession(SessionCreateCommand pCommand) throws Exception {
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
        } catch (Exception e) {
            log.error("aiService CS 질문 검증 호출 실패: {}", e.getMessage(), e);
            throw new BusinessException(AI_CALL_FAILED);
        }

        // 4. CS 관련 아닌 질문 필터링
        if (validationResponse == null || validationResponse.results() == null) {
            log.error("aiService CS 질문 검증 응답이 null 입니다.");
            throw new BusinessException(AI_CALL_FAILED);
        }

        List<String> invalidQuestions = validationResponse.results().stream()
                .filter(r -> !r.isValid())
                .map(CsValidationItemDTO::questionText)
                .toList();

        if (!invalidQuestions.isEmpty()) {
            log.warn("CS 관련 아닌 질문 포함: {}", invalidQuestions);
            throw new BusinessException(INVALID_CS_QUESTION);
        }

        // 5. DB 저장 (트랜잭션 범위)
        SessionCreateResponseDTO rDTO = sessionSaveService.saveManualSession(
                pCommand.toBuilder().sessionTitle(sessionTitle).build()
        );

        log.info("{}.createManualSession End!", this.getClass().getName());

        return rDTO;
    }

    private void validateCategories(List<CategoryItemRequestDTO> selectedCategories) {
        if (selectedCategories.isEmpty()) {
            throw new BusinessException(CATEGORY_REQUIRED);
        }

        // categoryType 화이트리스트 검증
        boolean hasInvalidType = selectedCategories.stream()
                .anyMatch(c -> !"COMMON_CS".equals(c.categoryType()) && !"JOB".equals(c.categoryType()));
        if (hasInvalidType) {
            throw new BusinessException(INVALID_CATEGORY_TYPE);
        }

        // categoryId 실존 검증 (userService OpenFeign + Caffeine 캐시)
        // getCategoryById() 내부에서 존재하지 않는 categoryId는 INVALID_CATEGORY_ID로 fail-close 처리
        selectedCategories.forEach(c -> csCategoryService.getCategoryById(c.categoryId()));
    }
}