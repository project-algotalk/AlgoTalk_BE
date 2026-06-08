package com.algotalk.interviewservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnswerEvaluationResponseDTO(
        Integer contentScore,           // 답변 논리성 점수 (0~25)
        FeedbackDTO feedback,           // 구조화된 피드백
        String modelAnswer,             // 모범 답변
        String studyTip,               // 학습 Tip
        List<String> followUpQuestions  // 꼬리 질문 예상 목록
) {}