package com.algotalk.interviewservice.domain;

import com.algotalk.interviewservice.dto.command.InterviewAnswerCommand;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Document(collection = "interview_analysis_report")
public class InterviewAnalysisDocument {

    @Id
    private String id;

    private Long sessionId;
    private Long sessionQuestionId;
    private Long userId;

    // STT 분석 결과
    private String answerText;
    private Integer answerDuration;
    private Integer wpm;
    private Double silenceRatio;
    private Double asrConfidence;
    private Integer fillerCount;
    private Double fillerRatio;

    // MediaPipe 분석 결과 (작업 시 업데이트)
    private Double gazeRatio;
    private List<GestureDeduction> gestureDeductions;

    // 점수 (MediaPipe 작업 후 업데이트)
    private Scores scores;

    private LocalDateTime createdAt;

    // Command -> Document 변환 팩토리 메서드
    public static InterviewAnalysisDocument from(InterviewAnswerCommand pCommand) {
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
                .gazeRatio(null)
                .gestureDeductions(null)
                .scores(null)
                .createdAt(LocalDateTime.now())
                .build();
    }
}