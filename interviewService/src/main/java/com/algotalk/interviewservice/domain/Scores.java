package com.algotalk.interviewservice.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Scores {
    private Integer gaze;     // 시선 점수 (25점)
    private Integer gesture;  // 제스처 점수 (20점)
    private Integer speed;    // 말하기 속도 점수 (15점)
    private Integer voice;    // 목소리/발음 점수 (15점)
    private Integer content;  // 답변 내용 점수 (25점)
    private Integer total;    // 총점 (100점)
}