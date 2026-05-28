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
}
