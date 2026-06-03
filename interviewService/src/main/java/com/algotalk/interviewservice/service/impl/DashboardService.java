package com.algotalk.interviewservice.service.impl;

import com.algotalk.common.pagination.Pagination;
import com.algotalk.interviewservice.domain.InterviewAnalysisDocument;
import com.algotalk.interviewservice.dto.command.InterviewSessionCommand;
import com.algotalk.interviewservice.dto.response.DashboardResponseDTO;
import com.algotalk.interviewservice.dto.response.ScoreDetailDTO;
import com.algotalk.interviewservice.dto.response.SessionSummaryDTO;
import com.algotalk.interviewservice.persistence.mongodb.IInterviewAnalysisMapper;
import com.algotalk.interviewservice.repository.IInterviewSessionMapper;
import com.algotalk.interviewservice.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService implements IDashboardService {

    private final IInterviewAnalysisMapper interviewAnalysisMapper;
    private final IInterviewSessionMapper interviewSessionMapper;

    @Override
    public DashboardResponseDTO getDashboard(Long userId, Pagination pagination) {
        log.info("{}.getDashboard Start!", this.getClass().getName());

        // 1. 총 세션 수
        int totalCount = interviewSessionMapper.getDashboardSessionCount(userId);

        // 2. 페이징된 세션 목록
        List<InterviewSessionCommand> sessions = interviewSessionMapper.getDashboardSessions(
                InterviewSessionCommand.builder()
                        .userId(userId)
                        .pagination(pagination)
                        .build()
        );

        // 3. 차트용 전체 세션 목록
        List<InterviewSessionCommand> allSessions = interviewSessionMapper.getDashboardSessions(
                InterviewSessionCommand.builder()
                        .userId(userId)
                        .pagination(Pagination.of(1, 1000, 1000))
                        .build()
        );

        // 4. MongoDB - userId 기준 전체 분석 결과 조회
        List<InterviewAnalysisDocument> allDocs = interviewAnalysisMapper.findByUserId(userId);

        // 5. 점수 있는 문서만 필터 (total > 0)
        List<InterviewAnalysisDocument> scoredDocs = allDocs.stream()
                .filter(doc -> doc.getScores() != null && safeScore(doc.getScores().total()) > 0)
                .toList();

        // 6. 전체 평균/최고 점수
        double avgScore = scoredDocs.isEmpty() ? 0.0
                : scoredDocs.stream()
                .mapToInt(doc -> safeScore(doc.getScores().total()))
                .average()
                .orElse(0.0);

        int maxScore = scoredDocs.isEmpty() ? 0
                : scoredDocs.stream()
                .mapToInt(doc -> safeScore(doc.getScores().total()))
                .max()
                .orElse(0);

        // 7. 세부 항목별 평균
        ScoreDetailDTO scoreDetails = ScoreDetailDTO.builder()
                .gaze(scoredDocs.isEmpty() ? 0.0
                        : scoredDocs.stream().mapToInt(d -> safeScore(d.getScores().gaze())).average().orElse(0.0))
                .gesture(scoredDocs.isEmpty() ? 0.0
                        : scoredDocs.stream().mapToInt(d -> safeScore(d.getScores().gesture())).average().orElse(0.0))
                .speed(scoredDocs.isEmpty() ? 0.0
                        : scoredDocs.stream().mapToInt(d -> safeScore(d.getScores().speed())).average().orElse(0.0))
                .voice(scoredDocs.isEmpty() ? 0.0
                        : scoredDocs.stream().mapToInt(d -> safeScore(d.getScores().voice())).average().orElse(0.0))
                .content(scoredDocs.isEmpty() ? 0.0
                        : scoredDocs.stream().mapToInt(d -> safeScore(d.getScores().content())).average().orElse(0.0))
                .build();

        // 8. sessionId별 평균 점수 (차트용)
        Map<Long, Double> sessionAvgMap = allDocs.stream()
                .filter(doc -> doc.getScores() != null)
                .collect(Collectors.groupingBy(
                        InterviewAnalysisDocument::getSessionId,
                        Collectors.averagingInt(doc -> safeScore(doc.getScores().total()))
                ));

        // 9. scoreHistory (차트용 전체)
        List<SessionSummaryDTO> scoreHistory = allSessions.stream()
                .map(s -> SessionSummaryDTO.builder()
                        .sessionId(s.getSessionId())
                        .sessionTitle(s.getSessionTitle())
                        .avgScore(Math.round(sessionAvgMap.getOrDefault(s.getSessionId(), 0.0) * 10) / 10.0)
                        .completedAt(s.getCompletedAt())
                        .build())
                .toList();

        // 10. recentSessions (페이징)
        List<SessionSummaryDTO> recentSessions = sessions.stream()
                .map(s -> SessionSummaryDTO.builder()
                        .sessionId(s.getSessionId())
                        .sessionTitle(s.getSessionTitle())
                        .avgScore(Math.round(sessionAvgMap.getOrDefault(s.getSessionId(), 0.0) * 10) / 10.0)
                        .totalQuestions(s.getTotalQuestions())
                        .completedAt(s.getCompletedAt())
                        .build())
                .toList();

        DashboardResponseDTO rDTO = DashboardResponseDTO.builder()
                .totalSessions(totalCount)
                .avgScore(Math.round(avgScore * 10) / 10.0)
                .maxScore(maxScore)
                .scoreDetails(scoreDetails)
                .scoreHistory(scoreHistory)
                .recentSessions(recentSessions)
                .page(pagination.getPage())
                .totalCount(totalCount)
                .build();

        log.info("{}.getDashboard End!", this.getClass().getName());
        return rDTO;
    }

    private int safeScore(Integer score) {
        return score != null ? score : 0;
    }
}