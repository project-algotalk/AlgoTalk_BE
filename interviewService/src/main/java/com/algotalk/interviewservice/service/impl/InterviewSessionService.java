package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.interviewservice.client.AiFeignClient;
import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.request.AiQuestionRequestDTO;
import com.algotalk.interviewservice.dto.request.CategoryItemRequestDTO;
import com.algotalk.interviewservice.dto.response.AiQuestionResponseDTO;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;
import com.algotalk.interviewservice.service.ICsCategoryService;
import com.algotalk.interviewservice.service.IInterviewSessionService;
import com.algotalk.interviewservice.service.ISessionSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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