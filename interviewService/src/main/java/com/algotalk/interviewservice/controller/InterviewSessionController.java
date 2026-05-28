package com.algotalk.interviewservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.command.SessionResultCommand;
import com.algotalk.interviewservice.dto.request.ManualSessionCreateRequestDTO;
import com.algotalk.interviewservice.dto.request.SessionCreateRequestDTO;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;
import com.algotalk.interviewservice.dto.response.SessionResultResponseDTO;
import com.algotalk.interviewservice.service.IInterviewSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping(value = "/interview/v1")
@RequiredArgsConstructor
public class InterviewSessionController {

    private final IInterviewSessionService interviewSessionService;

    @PostMapping("/sessions/llm")
    public ResponseEntity<ApiResponse<SessionCreateResponseDTO>> createSession(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody SessionCreateRequestDTO pDTO
            ) {
        log.info("{}.createSession Start!", this.getClass().getName());

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(userId)
                .selectedCategories(pDTO.selectedCategories())
                .questionCount(pDTO.questionCount())
                .build();

        SessionCreateResponseDTO rDTO = interviewSessionService.createSession(pCommand);

        log.info("{}.createSession End!", this.getClass().getName());

        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }

    @PostMapping("/sessions/manual")
    public ResponseEntity<ApiResponse<SessionCreateResponseDTO>> createManualSession(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ManualSessionCreateRequestDTO pDTO
    ) {
        log.info("{}.createManualSession Start!", this.getClass().getName());

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(userId)
                .manualQuestions(pDTO.questions())
                .build();

        SessionCreateResponseDTO rDTO = interviewSessionService.createManualSession(pCommand);

        log.info("{}.createManualSession End!", this.getClass().getName());

        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }

    @GetMapping("/sessions/{sessionId}/result")
    public ResponseEntity<ApiResponse<SessionResultResponseDTO>> getSessionResult(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long sessionId
    ) {
        log.info("{}.getSessionResult Start!", this.getClass().getName());

        SessionResultCommand pCommand = SessionResultCommand.builder()
                .sessionId(sessionId)
                .userId(userId)
                .build();

        SessionResultResponseDTO rDTO = interviewSessionService.getSessionResult(pCommand);

        log.info("{}.getSessionResult End!", this.getClass().getName());

        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }
}
