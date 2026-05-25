package com.algotalk.interviewservice.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class InterviewAnswerCommand {
    private Long userId;            // 사용자 ID
    private Long sessionId;         // 면접 세션 ID
    private Long sessionQuestionId; // 세션 질문 ID

    // STT 분석 결과
    private String answerText;      // 답변 텍스트 (STT 변환 결과)
    private Integer answerDuration; // 발화 시간 (초)
    private Integer wpm;            // 분당 발화 단어 수 (Words Per Minute)
    private Double silenceRatio;    // 무음 비율 (0.0 ~ 1.0)
    private Double asrConfidence;   // ASR 신뢰도 (0.0 ~ 1.0)
    private Integer fillerCount;    // 추임새 횟수 (예: "음", "어")
    private Double fillerRatio;     // 추임새 비율 (추임새 수 / 전체 단어 수)

    // MediaPipe 분석 결과
    private Double gazeRatio;                          // 시선 응시 비율 (0.0 ~ 1.0, 화면을 바라본 비율)
    private List<Map<String, Object>> gestureDeductions; // 제스처 감점 목록 (type, count, deduction)
}