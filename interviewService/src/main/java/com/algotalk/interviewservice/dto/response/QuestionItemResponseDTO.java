package com.algotalk.interviewservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionItemResponseDTO(
        Long sessionQuestionId,        // 세션 질문 ID
        int questionOrder,             // 질문 순서 (1부터 시작)
        String questionText,           // 질문 텍스트
        String sourceType,             // 질문 출처 (LLM_GENERATED / USER_INPUT / SCRAP_REFERENCE)
        String difficulty,             // 난이도 (EASY / MEDIUM / HARD, LLM_GENERATED 전용)
        String questionIntent,         // 출제 의도 (LLM_GENERATED 전용, 나머지는 null)
        List<String> questionKeywords  // 핵심 키워드 목록 (LLM_GENERATED 전용, 나머지는 null)
) {}