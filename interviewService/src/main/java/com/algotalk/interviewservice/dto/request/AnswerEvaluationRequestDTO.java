package com.algotalk.interviewservice.dto.request;

import lombok.Builder;

import java.util.List;

@Builder
public record AnswerEvaluationRequestDTO(
        String questionText,        // 질문 텍스트
        List<String> keywords,      // 핵심 키워드 목록
        String answerText           // 사용자 답변 텍스트
) {}