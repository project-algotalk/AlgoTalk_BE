package com.algotalk.interviewservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardResponseDTO {
    private Integer totalSessions;
    private Double avgScore;
    private Integer maxScore;
    private ScoreDetailDTO scoreDetails;
    private List<SessionSummaryDTO> scoreHistory;
    private List<SessionSummaryDTO> recentSessions;
    private Integer page;
    private Integer totalCount;
}