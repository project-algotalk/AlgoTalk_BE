package com.algotalk.interviewservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiQuestionResponseDTO(
        List<AiQuestionItemDTO> questions   // 생성된 질문 목록
) {
}
