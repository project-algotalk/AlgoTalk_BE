package com.algotalk.interviewservice.service.impl;

import com.algotalk.interviewservice.client.AiFeignClient;
import com.algotalk.interviewservice.domain.InterviewAnalysisDocument;
import com.algotalk.interviewservice.domain.Scores;
import com.algotalk.interviewservice.dto.command.EvaluationResultCommand;
import com.algotalk.interviewservice.dto.command.InterviewAnswerCommand;
import com.algotalk.interviewservice.dto.request.AnswerEvaluationRequestDTO;
import com.algotalk.interviewservice.dto.response.AnswerEvaluationResponseDTO;
import com.algotalk.interviewservice.exception.InterviewErrorCode;
import com.algotalk.interviewservice.persistence.mongodb.IInterviewAnalysisMapper;
import com.algotalk.interviewservice.service.IInterviewAnswerService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewAnswerService implements IInterviewAnswerService {

    private final IInterviewAnalysisMapper interviewAnalysisMapper;
    private final AiFeignClient aiFeignClient;

    // 목표 WPM 범위 (이 범위에 가까울수록 높은 점수)
    private static final int TARGET_WPM_MIN = 100;
    private static final int TARGET_WPM_MAX = 150;

    @Override
    public void saveAnswer(InterviewAnswerCommand pCommand) {
        log.info("{}.saveAnswer Start!", this.getClass().getName());

        // 1. speed 점수 계산 (WPM 기반, 0~15점)
        Integer speedScore = calcSpeedScore(pCommand.getWpm());

        // 2. voice 점수 계산 (추임새 비율 + 무음 비율 기반, 0~15점)
        Integer voiceScore = calcVoiceScore(pCommand.getFillerRatio(), pCommand.getSilenceRatio());

        // 3. scores 업데이트 (gaze, gesture는 FE에서 이미 계산되어 전달됨)
        Scores updatedScores = Scores.builder()
                .gaze(pCommand.getScores() != null ? pCommand.getScores().getGaze() : null)
                .gesture(pCommand.getScores() != null ? pCommand.getScores().getGesture() : null)
                .speed(speedScore)
                .voice(voiceScore)
                .content(null)   // LLM 평가 후 업데이트
                .total(null)     // LLM 평가 후 업데이트
                .build();

        // 4. MongoDB 저장
        InterviewAnalysisDocument rDoc = InterviewAnalysisDocument.from(
                pCommand.toBuilder().scores(updatedScores).build()
        );
        interviewAnalysisMapper.insertData(rDoc);

        // 5. aiService LLM 평가 요청 (비동기적으로 처리, 실패해도 저장은 정상 처리)
        try {
            if (pCommand.getAnswerText() != null && !pCommand.getAnswerText().isBlank()
                    && pCommand.getQuestionText() != null) {

                AnswerEvaluationResponseDTO evalResponse = aiFeignClient.evaluateAnswer(
                        AnswerEvaluationRequestDTO.builder()
                                .questionText(pCommand.getQuestionText())
                                .keywords(pCommand.getKeywords())
                                .answerText(pCommand.getAnswerText())
                                .build()
                );

                // 6. total 점수 계산
                Integer gaze = updatedScores.getGaze();
                Integer gesture = updatedScores.getGesture();
                Integer content = evalResponse.contentScore();
                Integer total = calcTotalScore(gaze, gesture, speedScore, voiceScore, content);

                // 7. MongoDB 업데이트 (content, feedback, modelAnswer, studyTip, followUpQuestions, total)
                interviewAnalysisMapper.updateEvaluationResult(
                        EvaluationResultCommand.builder()
                                .sessionQuestionId(pCommand.getSessionQuestionId())
                                .contentScore(evalResponse.contentScore())
                                .feedbackGood(evalResponse.feedback().good())
                                .feedbackImprove(evalResponse.feedback().improve())
                                .feedbackAddition(evalResponse.feedback().addition())
                                .modelAnswer(evalResponse.modelAnswer())
                                .studyTip(evalResponse.studyTip())
                                .followUpQuestions(evalResponse.followUpQuestions())
                                .total(total)
                                .build()
                );

                log.info("[EVAL_SUCCESS] sessionQuestionId={}, contentScore={}, total={}",
                        pCommand.getSessionQuestionId(), content, total);
            }
        } catch (FeignException e) {
            log.error("[{}] LLM 평가 실패 - sessionQuestionId={}, status={}",
                    InterviewErrorCode.AI_EVAL_FAILED.getCode(),
                    pCommand.getSessionQuestionId(), e.status(), e);
        } catch (Exception e) {
            log.error("[{}] LLM 평가 실패 - sessionQuestionId={}, message={}",
                    InterviewErrorCode.AI_EVAL_FAILED.getCode(),
                    pCommand.getSessionQuestionId(), e.getMessage(), e);
        }

        log.info("{}.saveAnswer End!", this.getClass().getName());
    }

    // WPM 기반 speed 점수 계산 (0~15점)
    // 목표 범위(100~150 WPM)에 가까울수록 높은 점수
    private Integer calcSpeedScore(Integer wpm) {
        if (wpm == null || wpm == 0) return 0;

        if (wpm >= TARGET_WPM_MIN && wpm <= TARGET_WPM_MAX) {
            return 15; // 목표 범위 내 → 만점
        } else if (wpm < TARGET_WPM_MIN) {
            // 너무 느림: 목표 최솟값 대비 비율로 감점
            return Math.max(0, (int) (15 * ((double) wpm / TARGET_WPM_MIN)));
        } else {
            // 너무 빠름: 목표 최댓값 초과분에 따라 감점
            int excess = wpm - TARGET_WPM_MAX;
            return Math.max(0, 15 - (excess / 10));
        }
    }

    // 추임새 비율 + 무음 비율 기반 voice 점수 계산 (0~15점)
    // 두 비율이 낮을수록 높은 점수
    private Integer calcVoiceScore(Double fillerRatio, Double silenceRatio) {
        if (fillerRatio == null || silenceRatio == null) return 0;

        // 추임새 비율 감점 (최대 7점 감점)
        int fillerPenalty = (int) Math.min(7, fillerRatio / 5);

        // 무음 비율 감점 (최대 8점 감점)
        int silencePenalty = (int) Math.min(8, silenceRatio / 10);

        return Math.max(0, 15 - fillerPenalty - silencePenalty);
    }

    // 전체 점수 계산 (0~100점)
    private Integer calcTotalScore(Integer gaze, Integer gesture,
                                   Integer speed, Integer voice, Integer content) {
        int total = 0;
        if (gaze != null) total += gaze;
        if (gesture != null) total += gesture;
        if (speed != null) total += speed;
        if (voice != null) total += voice;
        if (content != null) total += content;
        return total;
    }
}