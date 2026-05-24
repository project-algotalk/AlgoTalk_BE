package com.algotalk.interviewservice.persistence.mongodb;

import com.algotalk.interviewservice.domain.InterviewAnalysisDocument;

import java.util.List;
import java.util.Optional;

public interface IInterviewAnalysisMapper {

    // 답변 분석 결과 저장
    int insertData(InterviewAnalysisDocument pDoc) throws Exception;

    // sessionQuestionId로 분석 결과 조회
    Optional<InterviewAnalysisDocument> findBySessionQuestionId(Long sessionQuestionId) throws Exception;

    // sessionId로 전체 분석 결과 조회
    List<InterviewAnalysisDocument> findBySessionId(Long sessionId) throws Exception;
}