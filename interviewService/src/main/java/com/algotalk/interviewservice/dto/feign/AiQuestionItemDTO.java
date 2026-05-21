package com.algotalk.interviewservice.dto.feign;

import com.fasterxml.jackson.databind.introspect.Annotated;
import lombok.Builder;

import java.util.List;

@Builder
public record AiQuestionItemDTO(
        int order,
        String category,
        String difficulty,
        String content,
        String intent,
        List<String> keywords
) {
}
