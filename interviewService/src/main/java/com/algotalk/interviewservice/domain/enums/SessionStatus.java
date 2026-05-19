package com.algotalk.interviewservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SessionStatus {
    READY("READY", "세션이 준비된 상태"),
    IN_PROGRESS("IN_PROGRESS", "세션이 진행 중인 상태"),
    COMPLETED("COMPLETED", "세션이 완료된 상태");

    private final String status;
    private final String description;
}