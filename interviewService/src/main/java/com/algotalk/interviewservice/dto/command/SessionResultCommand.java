package com.algotalk.interviewservice.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SessionResultCommand {
    private Long sessionId;  // 면접 세션 ID
    private Long userId;     // 사용자 ID (권한 체크용)
}