package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.interviewservice.domain.enums.SourceType;
import com.algotalk.interviewservice.dto.command.InterviewSessionCommand;
import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.command.SessionQuestionCommand;
import com.algotalk.interviewservice.dto.request.CategoryItemRequestDTO;
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

import static com.algotalk.interviewservice.domain.enums.SessionStatus.READY;
import static com.algotalk.interviewservice.exception.InterviewErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewSessionService implements IInterviewSessionService {

    private final IInterviewSessionMapper interviewSessionMapper;
    private final ISessionQuestionMapper sessionQuestionMapper;

    @Transactional
    @Override
    public SessionCreateResponseDTO createSession(SessionCreateCommand pCommand) throws Exception {
        log.info("{}.createSession Start!", this.getClass().getName());

        // 1. selectedCategories 검증
        validateCategories(pCommand.getSelectedCategories());

        // 2. 면접 세션 INSERT
        InterviewSessionCommand sessionCommand = InterviewSessionCommand.builder()
                .userId(pCommand.getUserId())
                .sessionTitle(pCommand.getSessionTitle())
                .sessionStatus(READY.getStatus())
                .totalQuestions(pCommand.getQuestionCount())
                .build();

        interviewSessionMapper.insertInterviewSession(sessionCommand);
        Long sessionId = sessionCommand.getSessionId();

        // 3. 질문 생성 및 INSERT
        // TODO: aiService 연동 후 LLM 생성 질문으로 교체해야됨
        List<SessionQuestionCommand> questionCommands = buildDummyQuestions(
                sessionId, pCommand.getUserId(), pCommand.getQuestionCount()
        );
        for (SessionQuestionCommand questionCommand : questionCommands) {
            sessionQuestionMapper.insertSessionQuestion(questionCommand);
        }

        // 4. 저장된 질문 목록 조회
        List<SessionQuestionCommand> savedQuestions =
                sessionQuestionMapper.getSessionQuestionList(String.valueOf(sessionId));

        // 5. Response DTO 조립
        List<QuestionItemResponseDTO> questionItems = savedQuestions.stream()
                .map(q -> QuestionItemResponseDTO.builder()
                        .sessionQuestionId(q.getSessionQuestionId())
                        .questionOrder(q.getQuestionOrder())
                        .questionText(q.getQuestionText())
                        .sourceType(q.getSourceType())
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

    // validateCategories 메서드 수정
    private void validateCategories(List<CategoryItemRequestDTO> selectedCategories) {
        // 전체 카테고리 최소 1개 검증 (COMMON_CS + JOB 합산)
        if (selectedCategories.isEmpty()) {
            throw new BusinessException(CATEGORY_REQUIRED);
        }

        // categoryType 화이트리스트 검증
        boolean hasInvalidType = selectedCategories.stream()
                .anyMatch(c -> !"COMMON_CS".equals(c.categoryType()) && !"JOB".equals(c.categoryType()));
        if (hasInvalidType) {
            throw new BusinessException(INVALID_CATEGORY_TYPE);
        }
    }

    // aiService 연동 전 임시 더미 질문 생성
    private List<SessionQuestionCommand> buildDummyQuestions(Long sessionId, Long userId, int questionCount) {
        return java.util.stream.IntStream.rangeClosed(1, questionCount)
                .mapToObj(i -> SessionQuestionCommand.builder()
                        .sessionId(sessionId)
                        .userId(userId)
                        .questionText("임시 질문 " + i + "번입니다.")
                        .sourceType(SourceType.LLM_GENERATED.getType())
                        .questionOrder(i)
                        .questionIntent("임시 출제 의도")
                        .questionKeywords(List.of("키워드1", "키워드2"))
                        .build())
                .toList();
    }
}
