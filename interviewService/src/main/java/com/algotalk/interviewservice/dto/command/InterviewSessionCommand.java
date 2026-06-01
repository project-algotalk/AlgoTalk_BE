package com.algotalk.interviewservice.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionCommand {
    private Long sessionId;       // 세션 ID (INSERT 후 채워짐)
    private Long userId;          // 사용자 ID (Gateway X-User-Id 헤더에서 추출)
    private String sessionTitle;  // 세션 제목
    private String sessionStatus; // 세션 상태 (기본값: READY)
    private int totalQuestions;   // 총 질문 수
    private LocalDateTime completedAt;
    private Integer size;
    private Integer offset;
}