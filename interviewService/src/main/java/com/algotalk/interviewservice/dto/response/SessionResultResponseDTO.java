package com.algotalk.interviewservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SessionResultResponseDTO(
        Long sessionId,           // 면접 세션 ID
        String sessionTitle,      // 면접 세션 제목
        Integer totalQuestions,   // 전체 질문 수
        List<QuestionResultDTO> questions  // 질문별 결과
) {
}