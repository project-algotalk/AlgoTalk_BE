package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.interviewservice.client.AiFeignClient;
import com.algotalk.interviewservice.domain.enums.SourceType;
import com.algotalk.interviewservice.dto.command.InterviewSessionCommand;
import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.command.SessionQuestionCommand;
import com.algotalk.interviewservice.dto.feign.AiQuestionItemDTO;
import com.algotalk.interviewservice.dto.request.AiQuestionRequestDTO;
import com.algotalk.interviewservice.dto.request.CategoryItemRequestDTO;
import com.algotalk.interviewservice.dto.response.AiQuestionResponseDTO;
import com.algotalk.interviewservice.dto.response.QuestionItemResponseDTO;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;
import com.algotalk.interviewservice.repository.IInterviewSessionMapper;
import com.algotalk.interviewservice.repository.ISessionQuestionMapper;
import com.algotalk.interviewservice.service.IInterviewSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.algotalk.interviewservice.domain.enums.SessionStatus.READY;
import static com.algotalk.interviewservice.exception.InterviewErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService implements IInterviewSessionService {

    private final IInterviewSessionMapper interviewSessionMapper;
    private final ISessionQuestionMapper sessionQuestionMapper;
    private final AiFeignClient aiFeignClient;

    // categoryId → 카테고리명 변환 Map (임시 하드코딩)
    // TODO(interviewService): userService OpenFeign 연동 후 교체
    private static final Map<Long, String> CATEGORY_NAME_MAP = Map.ofEntries(
            Map.entry(10L, "자료구조/알고리즘"),
            Map.entry(11L, "데이터베이스"),
            Map.entry(12L, "운영체제"),
            Map.entry(13L, "네트워크"),
            Map.entry(20L, "소프트웨어 개발"),
            Map.entry(21L, "데이터 및 인공지능"),
            Map.entry(22L, "IT 인프라 및 운영"),
            Map.entry(23L, "IT 기획 및 서비스"),
            Map.entry(100L, "프론트엔드 개발자"),
            Map.entry(101L, "백엔드 개발자"),
            Map.entry(102L, "풀스택 개발자"),
            Map.entry(103L, "모바일/앱 개발자"),
            Map.entry(104L, "게임 개발자"),
            Map.entry(105L, "임베디드 SW 개발자"),
            Map.entry(110L, "AI/머신러닝 엔지니어"),
            Map.entry(111L, "데이터 사이언티스트"),
            Map.entry(112L, "데이터 엔지니어"),
            Map.entry(113L, "프롬프트 엔지니어"),
            Map.entry(120L, "클라우드 엔지니어"),
            Map.entry(121L, "DevOps/SRE 엔지니어"),
            Map.entry(122L, "정보보안 전문가"),
            Map.entry(123L, "네트워크/시스템 관리자"),
            Map.entry(130L, "IT 프로젝트 매니저(PM)"),
            Map.entry(131L, "서비스 기획자/PO"),
            Map.entry(132L, "UI/UX 디자이너"),
            Map.entry(133L, "QA 엔지니어")
            // 24L(기타/직접입력)은 의도적으로 제외 - 면접 세션 생성 시 허용하지 않음
    );

    @Override
    public SessionCreateResponseDTO createSession(SessionCreateCommand pCommand) throws Exception {
        log.info("{}.createSession Start!", this.getClass().getName());

        // 1. selectedCategories 검증
        validateCategories(pCommand.getSelectedCategories());

        // 2. categoryId → 카테고리명 변환 (@Transactional 밖에서 수행)
        List<String> categoryNames = pCommand.getSelectedCategories().stream()
                .map(c -> {
                    String categoryName = CATEGORY_NAME_MAP.get(c.categoryId());
                    if (categoryName == null) {
                        // TODO: userService 연동 후 실존 검증으로 교체
                        throw new BusinessException(INVALID_CATEGORY_TYPE);
                    }
                    return categoryName;
                })
                .toList();

        // 3. aiService 호출하여 LLM 질문 생성 (@Transactional 밖에서 수행)
        AiQuestionRequestDTO aiRequest = AiQuestionRequestDTO.builder()
                .categories(categoryNames)
                .questionCount(pCommand.getQuestionCount())
                .build();

        AiQuestionResponseDTO aiResponse = aiFeignClient.generateQuestions(aiRequest);

        // 4. aiService 응답 검증
        if (aiResponse == null || aiResponse.questions() == null || aiResponse.questions().isEmpty()) {
            throw new BusinessException(QUESTION_INSERT_FAILED);
        }
        if (aiResponse.questions().size() != pCommand.getQuestionCount()) {
            throw new BusinessException(QUESTION_INSERT_FAILED);
        }

        // 5. DB 저장 (트랜잭션 범위)
        return saveSession(pCommand, aiResponse.questions());
    }

    @Transactional
    public SessionCreateResponseDTO saveSession(SessionCreateCommand pCommand,
                                                List<AiQuestionItemDTO> aiQuestions) throws Exception {
        // 1. 면접 세션 INSERT
        InterviewSessionCommand sessionCommand = InterviewSessionCommand.builder()
                .userId(pCommand.getUserId())
                .sessionTitle(pCommand.getSessionTitle())
                .sessionStatus(READY.getStatus())
                .totalQuestions(pCommand.getQuestionCount())
                .build();

        interviewSessionMapper.insertInterviewSession(sessionCommand);
        Long sessionId = sessionCommand.getSessionId();

        // 2. 생성된 질문 INSERT
        for (AiQuestionItemDTO aiQuestion : aiQuestions) {
            SessionQuestionCommand questionCommand = SessionQuestionCommand.builder()
                    .sessionId(sessionId)
                    .userId(pCommand.getUserId())
                    .questionText(aiQuestion.content())
                    .sourceType(SourceType.LLM_GENERATED.getType())
                    .questionOrder(aiQuestion.order())
                    .difficulty(aiQuestion.difficulty())
                    .questionIntent(aiQuestion.intent())
                    .questionKeywords(aiQuestion.keywords())
                    .build();

            int res = sessionQuestionMapper.insertSessionQuestion(questionCommand);
            if (res != 1) {
                throw new BusinessException(QUESTION_INSERT_FAILED);
            }
        }

        // 3. 저장된 질문 목록 조회
        List<SessionQuestionCommand> savedQuestions =
                sessionQuestionMapper.getSessionQuestionList(sessionCommand);

        // 4. Response DTO 조립
        List<QuestionItemResponseDTO> questionItems = savedQuestions.stream()
                .map(q -> QuestionItemResponseDTO.builder()
                        .sessionQuestionId(q.getSessionQuestionId())
                        .questionOrder(q.getQuestionOrder())
                        .questionText(q.getQuestionText())
                        .sourceType(q.getSourceType())
                        .difficulty(q.getDifficulty())
                        .questionIntent(q.getQuestionIntent())
                        .questionKeywords(q.getQuestionKeywords())
                        .build())
                .toList();

        SessionCreateResponseDTO rDTO = SessionCreateResponseDTO.builder()
                .sessionId(sessionId)
                .sessionTitle(pCommand.getSessionTitle())
                .status(READY.getStatus())
                .totalQuestions(pCommand.getQuestionCount())
                .questions(questionItems)
                .createdAt(LocalDateTime.now())
                .build();

        log.info("{}.createSession End!", this.getClass().getName());

        return rDTO;
    }

    private void validateCategories(List<CategoryItemRequestDTO> selectedCategories) {
        if (selectedCategories.isEmpty()) {
            throw new BusinessException(CATEGORY_REQUIRED);
        }

        boolean hasInvalidType = selectedCategories.stream()
                .anyMatch(c -> !"COMMON_CS".equals(c.categoryType()) && !"JOB".equals(c.categoryType()));
        if (hasInvalidType) {
            throw new BusinessException(INVALID_CATEGORY_TYPE);
        }

        // TODO(interviewService): categoryId 실존 검증 추가
        // - userService GET /cs-categories/v1 (OpenFeign)로 활성 카테고리 목록 조회
        // - 조회 결과를 Caffeine 로컬 캐시(짧은 TTL, 예: 1~5분) 후 categoryId 포함 여부 검증
        // - userService 조회 실패/타임아웃 또는 미존재 categoryId는 fail-close로 BusinessException 처리
    }
}