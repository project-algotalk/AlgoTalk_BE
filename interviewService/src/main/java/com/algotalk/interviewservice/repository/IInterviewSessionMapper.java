package com.algotalk.interviewservice.repository;

import com.algotalk.interviewservice.dto.command.InterviewSessionCommand;
import com.algotalk.interviewservice.dto.command.SessionResultCommand;
import com.algotalk.interviewservice.dto.response.SessionResultResponseDTO;
import com.algotalk.interviewservice.dto.row.SessionResultRowDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IInterviewSessionMapper {
    // 면접 세션 생성
    int insertInterviewSession(InterviewSessionCommand pCommand);

    // 면접 세션 + 질문 목록 조회 (결과 페이지용)
    List<SessionResultRowDTO> getSessionResult(SessionResultCommand pCommand);

    // 대시보드용 세션 목록 조회
    List<InterviewSessionCommand> getDashboardSessions(InterviewSessionCommand pCommand);

    // 대시보드용 총 세션 수
    int getDashboardSessionCount(Long userId);

    // 면접 세션 종료
    void completeSession(Long sessionId);
}
