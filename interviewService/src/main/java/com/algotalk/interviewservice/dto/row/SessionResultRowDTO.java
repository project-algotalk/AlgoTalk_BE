package com.algotalk.interviewservice.dto.row;

import lombok.Builder;

// 조회용 DTO
@Builder
public record SessionResultRowDTO(
        Long sessionId,
        String sessionTitle,
        Integer totalQuestions,
        Long sessionQuestionId,
        Integer questionOrder,
        String questionText
) {
}