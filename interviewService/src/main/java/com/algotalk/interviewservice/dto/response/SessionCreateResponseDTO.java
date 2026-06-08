package com.algotalk.interviewservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionCreateResponseDTO(
        Long sessionId,                        // 생성된 세션 ID
        String sessionTitle,                   // 세션 제목
        String status,                         // 세션 상태 (READY / IN_PROGRESS / COMPLETED)
        int totalQuestions,                    // 총 질문 수
        List<QuestionItemResponseDTO> questions, // 생성된 질문 목록
        LocalDateTime createdAt                // 세션 생성 일시
) {}