package com.algotalk.interviewservice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScoreDetailDTO {
    private Double gaze;
    private Double gesture;
    private Double speed;
    private Double voice;
    private Double content;
}