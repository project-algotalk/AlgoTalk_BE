package com.algotalk.interviewservice.dto.request;

import com.algotalk.interviewservice.domain.Scores;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record InterviewAnswerRequestDTO(

        @NotNull(message = "답변 텍스트는 필수입니다.")
        String answerText,

        @NotNull(message = "발화 시간은 필수입니다.")
        Integer answerDuration,

        @NotNull(message = "WPM은 필수입니다.")
        Integer wpm,

        @NotNull(message = "무음 비율은 필수입니다.")
        Double silenceRatio,

        @NotNull(message = "ASR 신뢰도는 필수입니다.")
        Double asrConfidence,

        @NotNull(message = "추임새 횟수는 필수입니다.")
        Integer fillerCount,

        @NotNull(message = "추임새 비율은 필수입니다.")
        Double fillerRatio,

        Double gazeRatio,           // 시선 응시 비율 (0.0 ~ 1.0)

        List<Map<String, Object>> gestureDeductions,  // 제스처 감점 목록

        Scores scores
) {}