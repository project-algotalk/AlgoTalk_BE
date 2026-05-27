package com.algotalk.interviewservice.dto.response;

import com.algotalk.interviewservice.domain.GestureDeduction;
import com.algotalk.interviewservice.domain.InterviewAnalysisDocument;
import com.algotalk.interviewservice.domain.Scores;
import com.algotalk.interviewservice.domain.enums.AnswerStatus;
import lombok.Builder;

import java.util.List;

@Builder
public record QuestionResultDTO(
        Long sessionQuestionId,   // 세션 질문 ID
        Integer questionOrder,    // 질문 순서
        String questionText,      // 질문 텍스트
        AnswerStatus answerStatus, // 답변 상태

        // STT 분석 결과
        String answerText,        // 답변 텍스트
        Integer answerDuration,   // 발화 시간 (초)
        Integer wpm,              // 말하기 속도
        Double silenceRatio,      // 무음 비율
        Integer fillerCount,      // 추임새 횟수
        Double fillerRatio,       // 추임새 비율

        // MediaPipe 분석 결과
        Double gazeRatio,                         // 시선 응시 비율
        List<GestureDeduction> gestureDeductions, // 제스처 감점 내역

        // 점수
        Scores scores,            // 항목별 점수

        // LLM 평가 결과
        Integer contentScore,     // 답변 논리성 점수
        String feedbackGood,      // 잘한 점
        String feedbackImprove,   // 부족한 점
        String feedbackAddition,  // 추가할 내용
        String modelAnswer,       // 모범 답변
        String studyTip,          // 학습 Tip
        List<String> followUpQuestions  // 꼬리 질문 목록
) {
    // 답변 있는 경우: MongoDB document + 질문 정보로 변환
    public static QuestionResultDTO from(InterviewAnalysisDocument doc, Integer questionOrder, String questionText) {
        return QuestionResultDTO.builder()
                .sessionQuestionId(doc.getSessionQuestionId())
                .questionOrder(questionOrder)
                .questionText(questionText)
                .answerStatus(doc.getAnswerStatus())
                .answerText(doc.getAnswerText())
                .answerDuration(doc.getAnswerDuration())
                .wpm(doc.getWpm())
                .silenceRatio(doc.getSilenceRatio())
                .fillerCount(doc.getFillerCount())
                .fillerRatio(doc.getFillerRatio())
                .gazeRatio(doc.getGazeRatio())
                .gestureDeductions(doc.getGestureDeductions())
                .scores(doc.getScores())
                .contentScore(doc.getContentScore())
                .feedbackGood(doc.getFeedbackGood())
                .feedbackImprove(doc.getFeedbackImprove())
                .feedbackAddition(doc.getFeedbackAddition())
                .modelAnswer(doc.getModelAnswer())
                .studyTip(doc.getStudyTip())
                .followUpQuestions(doc.getFollowUpQuestions())
                .build();
    }

    // 답변 없는 경우: 질문 정보만으로 변환
    public static QuestionResultDTO empty(Long sessionQuestionId, Integer questionOrder, String questionText) {
        return QuestionResultDTO.builder()
                .sessionQuestionId(sessionQuestionId)
                .questionOrder(questionOrder)
                .questionText(questionText)
                .answerStatus(null)
                .build();
    }
}