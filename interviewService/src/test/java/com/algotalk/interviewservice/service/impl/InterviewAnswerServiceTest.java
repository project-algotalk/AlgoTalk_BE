package com.algotalk.interviewservice.service.impl;

import com.algotalk.interviewservice.client.AiFeignClient;
import com.algotalk.interviewservice.domain.InterviewAnalysisDocument;
import com.algotalk.interviewservice.dto.command.InterviewAnswerCommand;
import com.algotalk.interviewservice.dto.response.AnswerEvaluationResponseDTO;
import com.algotalk.interviewservice.dto.response.FeedbackDTO;
import com.algotalk.interviewservice.persistence.mongodb.IInterviewAnalysisMapper;
import com.algotalk.interviewservice.service.IInterviewAnswerService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InterviewAnswerServiceTest {

    @Autowired
    private IInterviewAnswerService interviewAnswerService;

    @Autowired
    private IInterviewAnalysisMapper interviewAnalysisMapper;

    @MockBean
    private AiFeignClient aiFeignClient;

    @Test
    @DisplayName("답변 저장 성공 - MongoDB 저장 확인")
    void saveAnswer_success() throws Exception {
        // given
        InterviewAnswerCommand pCommand = InterviewAnswerCommand.builder()
                .userId(1L)
                .sessionId(134L)
                .sessionQuestionId(381L)
                .questionText("프로세스와 스레드의 차이를 설명하세요.")  // ← 추가
                .keywords(List.of("프로세스", "스레드", "자원 공유"))    // ← 추가
                .answerText("프로세스는 독립적인 메모리 공간을 가지며 스레드는 공유합니다.")
                .answerDuration(45)
                .wpm(135)
                .silenceRatio(8.5)
                .asrConfidence(0.923)
                .fillerCount(2)
                .fillerRatio(5.13)
                .build();

        // aiService 평가 Mock 설정
        when(aiFeignClient.evaluateAnswer(any())).thenReturn(
                AnswerEvaluationResponseDTO.builder()
                        .contentScore(20)
                        .feedback(FeedbackDTO.builder()
                                .good("잘한 점입니다.")
                                .improve("부족한 점입니다.")
                                .addition("추가할 내용입니다.")
                                .build())
                        .modelAnswer("모범 답변입니다.")
                        .studyTip("학습 Tip입니다.")
                        .followUpQuestions(List.of("꼬리 질문 1", "꼬리 질문 2"))
                        .build()
        );

        // when
        interviewAnswerService.saveAnswer(pCommand);

        // then
        Optional<InterviewAnalysisDocument> rDoc =
                interviewAnalysisMapper.findBySessionQuestionId(381L);

        assertThat(rDoc).isPresent();
        assertThat(rDoc.get().getSessionId()).isEqualTo(134L);
        assertThat(rDoc.get().getAnswerText()).isEqualTo("프로세스는 독립적인 메모리 공간을 가지며 스레드는 공유합니다.");
        assertThat(rDoc.get().getWpm()).isEqualTo(135);
        assertThat(rDoc.get().getFillerCount()).isEqualTo(2);
        assertThat(rDoc.get().getScores()).isNotNull();
        assertThat(rDoc.get().getScores().speed()).isNotNull();
        assertThat(rDoc.get().getScores().voice()).isNotNull();

        log.info("저장된 Document: {}", rDoc.get());
    }
}