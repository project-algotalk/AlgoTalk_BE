package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.interviewservice.domain.enums.SourceType;
import com.algotalk.interviewservice.dto.command.InterviewSessionCommand;
import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.command.SessionQuestionCommand;
import com.algotalk.interviewservice.dto.response.AiQuestionItemDTO;
import com.algotalk.interviewservice.dto.request.ManualQuestionItemRequestDTO;
import com.algotalk.interviewservice.dto.response.QuestionItemResponseDTO;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;
import com.algotalk.interviewservice.repository.IInterviewSessionMapper;
import com.algotalk.interviewservice.repository.ISessionQuestionMapper;
import com.algotalk.interviewservice.service.ISessionSaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.algotalk.interviewservice.domain.enums.SessionStatus.READY;
import static com.algotalk.interviewservice.exception.InterviewErrorCode.QUESTION_INSERT_FAILED;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSaveService implements ISessionSaveService {

    private final IInterviewSessionMapper interviewSessionMapper;
    private final ISessionQuestionMapper sessionQuestionMapper;

    @Transactional
    @Override
    public SessionCreateResponseDTO saveSession(SessionCreateCommand pCommand,
                                                List<AiQuestionItemDTO> aiQuestions) {
        log.info("{}.saveSession Start!", this.getClass().getName());
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
            // aiQuestion.category()로 categoryId 매핑
            Long categoryId = pCommand.getCategoryNames().indexOf(aiQuestion.category()) >= 0
                    ? pCommand.getSelectedCategories()
                    .get(pCommand.getCategoryNames().indexOf(aiQuestion.category()))
                    .categoryId()
                    : null;

            SessionQuestionCommand questionCommand = SessionQuestionCommand.builder()
                    .sessionId(sessionId)
                    .userId(pCommand.getUserId())
                    .categoryId(categoryId)             // ← 추가
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

        log.info("{}.saveSession End!", this.getClass().getName());

        return rDTO;
    }

    @Transactional
    @Override
    public SessionCreateResponseDTO saveManualSession(SessionCreateCommand pCommand) {
        log.info("{}.saveManualSession Start!", this.getClass().getName());

        // 1. 면접 세션 INSERT
        InterviewSessionCommand sessionCommand = InterviewSessionCommand.builder()
                .userId(pCommand.getUserId())
                .sessionTitle(pCommand.getSessionTitle())
                .sessionStatus(READY.getStatus())
                .totalQuestions(pCommand.getManualQuestions().size())
                .build();

        interviewSessionMapper.insertInterviewSession(sessionCommand);
        Long sessionId = sessionCommand.getSessionId();

        // 2. 질문 INSERT (difficulty, questionIntent, questionKeywords = null)
        List<ManualQuestionItemRequestDTO> questions = pCommand.getManualQuestions();
        for (int i = 0; i < questions.size(); i++) {
            ManualQuestionItemRequestDTO q = questions.get(i);
            SessionQuestionCommand questionCommand = SessionQuestionCommand.builder()
                    .sessionId(sessionId)
                    .userId(pCommand.getUserId())
                    .questionText(q.questionText())
                    .sourceType(SourceType.USER_INPUT.getType())
                    .questionOrder(i + 1)
                    .difficulty(null)
                    .questionIntent(null)
                    .questionKeywords(null)
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
                .totalQuestions(pCommand.getManualQuestions().size())
                .questions(questionItems)
                .createdAt(LocalDateTime.now())
                .build();

        log.info("{}.saveManualSession End!", this.getClass().getName());

        return rDTO;
    }
}