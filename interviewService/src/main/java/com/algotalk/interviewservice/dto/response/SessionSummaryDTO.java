package com.algotalk.interviewservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionSummaryDTO {
    private Long sessionId;
    private String sessionTitle;
    private Double avgScore;
    private Integer totalQuestions;
    private LocalDateTime completedAt;
}