package com.algotalk.interviewservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiQuestionItemDTO(
        int order,
        String category,
        String difficulty,
        String content,
        String intent,
        List<String> keywords
) {
}
