package com.algotalk.interviewservice.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record AiQuestionResponseDTO(
        List<AiQuestionItemDTO> questions   // 생성된 질문 목록
) {
}
