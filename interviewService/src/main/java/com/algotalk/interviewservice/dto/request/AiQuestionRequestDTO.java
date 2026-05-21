package com.algotalk.interviewservice.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record AiQuestionRequestDTO(
        List<String> categories,   // 질문 카테고리 목록 (예: ["알고리즘", "자료구조"])
        int questionCount          // 생성할 질문 수
) {
}
