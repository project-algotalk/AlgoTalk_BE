package com.algotalk.interviewservice.repository;

import com.algotalk.interviewservice.dto.command.SessionQuestionCommand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ISessionQuestionMapper {

    // 세션 질문 단건 저장
    int insertSessionQuestion(SessionQuestionCommand pCommand) throws Exception;

    // 세션 질문 목록 조회
    List<SessionQuestionCommand> getSessionQuestionList(String sessionId);
}
