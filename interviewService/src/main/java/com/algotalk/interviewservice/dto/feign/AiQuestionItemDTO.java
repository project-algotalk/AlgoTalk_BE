package com.algotalk.interviewservice.dto.feign;

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
