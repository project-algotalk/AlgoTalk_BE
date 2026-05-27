package com.algotalk.interviewservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.interviewservice.dto.command.InterviewAnswerCommand;
import com.algotalk.interviewservice.dto.request.InterviewAnswerRequestDTO;
import com.algotalk.interviewservice.service.IInterviewAnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/interview/v1/sessions")
@RequiredArgsConstructor
public class InterviewAnswerController {

    private final IInterviewAnswerService interviewAnswerService;

    @PostMapping("/{sessionId}/questions/{sessionQuestionId}/answer")
    public ResponseEntity<ApiResponse<Void>> saveAnswer(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long sessionId,
            @PathVariable Long sessionQuestionId,
            @Valid @RequestBody InterviewAnswerRequestDTO pDTO
    ) {
        log.info("{}.saveAnswer Start!", this.getClass().getName());

        InterviewAnswerCommand pCommand = InterviewAnswerCommand.builder()
                .userId(userId)
                .sessionId(sessionId)
                .sessionQuestionId(sessionQuestionId)
                .answerStatus(pDTO.answerStatus())
                .questionText(pDTO.questionText())
                .keywords(pDTO.keywords())
                .answerText(pDTO.answerText())
                .answerDuration(pDTO.answerDuration())
                .wpm(pDTO.wpm())
                .silenceRatio(pDTO.silenceRatio())
                .asrConfidence(pDTO.asrConfidence())
                .fillerCount(pDTO.fillerCount())
                .fillerRatio(pDTO.fillerRatio())
                .gazeRatio(pDTO.gazeRatio())
                .gestureDeductions(pDTO.gestureDeductions())
                .scores(pDTO.scores())
                .build();

        interviewAnswerService.saveAnswer(pCommand);

        log.info("{}.saveAnswer End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}