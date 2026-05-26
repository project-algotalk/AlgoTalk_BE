package com.algotalk.interviewservice.domain;

import com.algotalk.interviewservice.dto.command.InterviewAnswerCommand;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
@Document(collection = "interview_analysis_report")
public class InterviewAnalysisDocument {

    private Long sessionId;           // 면접 세션 ID
    private Long sessionQuestionId;   // 세션 질문 ID
    private Long userId;              // 사용자 ID

    // STT 분석 결과
    private String answerText;        // 답변 텍스트 (STT 변환 결과)
    private Integer answerDuration;   // 발화 시간 (초)
    private Integer wpm;              // 분당 발화 단어 수 (Words Per Minute)
    private Double silenceRatio;      // 무음 비율 (0.0 ~ 1.0)
    private Double asrConfidence;     // ASR 신뢰도 (0.0 ~ 1.0)
    private Integer fillerCount;      // 추임새 횟수 (예: "음", "어")
    private Double fillerRatio;       // 추임새 비율 (추임새 수 / 전체 단어 수)

    // MediaPipe 분석 결과
    private Double gazeRatio;                      // 시선 응시 비율 (0.0 ~ 1.0, 화면을 바라본 비율)
    private List<GestureDeduction> gestureDeductions; // 제스처 감점 목록

    // 점수 (추후 업데이트)
    private Scores scores;

    private LocalDateTime createdAt;  // 저장 일시

    // Command -> Document 변환 팩토리 메서드
    public static InterviewAnalysisDocument from(InterviewAnswerCommand pCommand) {

        // gestureDeductions: List<Map<String, Object>> -> List<GestureDeduction> 변환
        List<GestureDeduction> gestureDeductions = null;
        if (pCommand.getGestureDeductions() != null) {
            gestureDeductions = pCommand.getGestureDeductions().stream()
                    .map(m -> GestureDeduction.builder()
                            .type((String) m.get("type"))
                            .count(((Number) m.get("count")).intValue())
                            .deduction(((Number) m.get("deduction")).intValue())
                            .build())
                    .toList();
        }

        return InterviewAnalysisDocument.builder()
                .sessionId(pCommand.getSessionId())
                .sessionQuestionId(pCommand.getSessionQuestionId())
                .userId(pCommand.getUserId())
                .answerText(pCommand.getAnswerText())
                .answerDuration(pCommand.getAnswerDuration())
                .wpm(pCommand.getWpm())
                .silenceRatio(pCommand.getSilenceRatio())
                .asrConfidence(pCommand.getAsrConfidence())
                .fillerCount(pCommand.getFillerCount())
                .fillerRatio(pCommand.getFillerRatio())
                .gazeRatio(pCommand.getGazeRatio())
                .gestureDeductions(gestureDeductions)
                .scores(pCommand.getScores())
                .createdAt(LocalDateTime.now())
                .build();
    }
}