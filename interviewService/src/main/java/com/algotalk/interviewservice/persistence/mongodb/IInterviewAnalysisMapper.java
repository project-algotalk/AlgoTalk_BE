package com.algotalk.interviewservice.persistence.mongodb;

import com.algotalk.interviewservice.domain.InterviewAnalysisDocument;
import com.algotalk.interviewservice.dto.command.EvaluationResultCommand;
import com.algotalk.interviewservice.dto.command.SessionResultCommand;

import java.util.List;
import java.util.Optional;

public interface IInterviewAnalysisMapper {

    // 답변 분석 결과 저장
    int insertData(InterviewAnalysisDocument pDoc);

    // sessionQuestionId로 분석 결과 조회
    Optional<InterviewAnalysisDocument> findBySessionQuestionId(Long sessionQuestionId);

    // sessionId로 전체 분석 결과 조회
    List<InterviewAnalysisDocument> findBySessionId(SessionResultCommand pCommand);

    // LLM 평가 결과 업데이트
    void updateEvaluationResult(EvaluationResultCommand pCommand);
}