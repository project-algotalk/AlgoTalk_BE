package com.algotalk.interviewservice.domain;

import lombok.Builder;
import lombok.Getter;

@Builder
public record Scores(
        Integer gaze,     // 시선 점수 (25점)
        Integer gesture,  // 제스처 점수 (20점)
        Integer speed,    // 말하기 속도 점수 (15점)
        Integer voice,    // 목소리/발음 점수 (15점)
        Integer content,  // 답변 내용 점수 (25점)
        Integer total    // 총점 (100점)
) {
    public Scores {
        if (gaze == null) gaze = 0;
        if (gesture == null) gesture = 0;
        if (speed == null) speed = 0;
        if (voice == null) voice = 0;
        // TODO: content, total은 나중에 계산할 예정
//        if (content == null) content = 0;
//        if (total == null) total = gaze + gesture + speed + voice + content;
    }
}