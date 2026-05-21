package com.algotalk.interviewservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.request.SessionCreateRequestDTO;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;
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
            ) throws Exception {
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
}
