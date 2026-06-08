package com.algotalk.interviewservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum AnswerStatus {
    ANSWERED("ANSWERED", "정상 답변"),
    SKIPPED("SKIPPED", "건너뛰기"),
    QUALITY_FAIL("QUALITY_FAIL", "품질 미달");

    private final String status;
    private final String description;
}
