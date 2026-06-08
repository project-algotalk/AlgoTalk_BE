package com.algotalk.interviewservice.repository;

import com.algotalk.interviewservice.dto.command.InterviewSessionCommand;
import com.algotalk.interviewservice.dto.command.RecentQuestionSearchCommand;
import com.algotalk.interviewservice.dto.command.SessionQuestionCommand;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ISessionQuestionMapper {

    // 세션 질문 단건 저장
    int insertSessionQuestion(SessionQuestionCommand pCommand);

    // 세션 질문 목록 조회
    List<SessionQuestionCommand> getSessionQuestionList(InterviewSessionCommand pCommand);

    // 유저별 최근 출제 질문 텍스트 조회 (히스토리 기반 재출제 방지용)
    List<String> findRecentQuestionsByUserAndCategories(RecentQuestionSearchCommand pCommand);
}
