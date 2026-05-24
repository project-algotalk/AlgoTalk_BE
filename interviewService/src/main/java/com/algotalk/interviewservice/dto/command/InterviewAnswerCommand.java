package com.algotalk.interviewservice.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InterviewAnswerCommand {
    private Long userId;
    private Long sessionId;
    private Long sessionQuestionId;

    // STT 분석 결과
    private String answerText;
    private Integer answerDuration;
    private Integer wpm;
    private Double silenceRatio;
    private Double asrConfidence;
    private Integer fillerCount;
    private Double fillerRatio;
}