package com.algotalk.interviewservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record InterviewAnswerRequestDTO(
        @NotBlank(message = "답변 텍스트는 공백일 수 없습니다.")
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
        Double fillerRatio
) {}