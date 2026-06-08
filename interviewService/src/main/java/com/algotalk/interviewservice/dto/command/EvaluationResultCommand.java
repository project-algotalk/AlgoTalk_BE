package com.algotalk.interviewservice.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class EvaluationResultCommand {
    private Long sessionQuestionId;     // 세션 질문 ID
    private Integer contentScore;       // 답변 논리성 점수 (0~25)
    private String feedbackGood;        // 피드백 - 잘한 점
    private String feedbackImprove;     // 피드백 - 부족한 점
    private String feedbackAddition;    // 피드백 - 추가할 내용
    private String modelAnswer;         // 모범 답변
    private String studyTip;            // 학습 Tip
    private List<String> followUpQuestions; // 꼬리 질문 예상 목록
    private Integer total;              // 종합 점수 (0~100)
}