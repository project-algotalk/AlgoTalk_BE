package com.algotalk.interviewservice.repository;

import com.algotalk.interviewservice.dto.command.InterviewSessionCommand;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IInterviewSessionMapper {
    // 면접 세션 생성
    int insertInterviewSession(InterviewSessionCommand pCommand);
}
