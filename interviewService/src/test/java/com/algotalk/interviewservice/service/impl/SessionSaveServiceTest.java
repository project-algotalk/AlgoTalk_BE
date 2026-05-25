package com.algotalk.interviewservice.service.impl;

import com.algotalk.interviewservice.dto.command.SessionCreateCommand;
import com.algotalk.interviewservice.dto.feign.AiQuestionItemDTO;
import com.algotalk.interviewservice.dto.request.CategoryItemRequestDTO;
import com.algotalk.interviewservice.dto.response.SessionCreateResponseDTO;
import com.algotalk.interviewservice.service.ISessionSaveService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionSaveServiceTest {

    @Autowired
    private ISessionSaveService sessionSaveService;

    @Test
    @Transactional
    @DisplayName("세션 저장 성공 - 카테고리 1개 저장 확인")
    void saveSession_success_singleCategory() throws Exception {
        // given
        List<AiQuestionItemDTO> questions = List.of(
                AiQuestionItemDTO.builder()
                        .order(1)
                        .category("백엔드 개발자")
                        .difficulty("MEDIUM")
                        .content("테스트 질문입니다.")
                        .intent("테스트 출제 의도")
                        .keywords(List.of("키워드1", "키워드2"))
                        .build()
        );

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .sessionTitle("백엔드 개발자 모의면접")
                .questionCount(1)
                .selectedCategories(List.of(
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L)
                                .categoryType("JOB")
                                .build()
                ))
                .categoryNames(List.of("백엔드 개발자"))
                .build();

        // when
        SessionCreateResponseDTO rDTO = sessionSaveService.saveSession(pCommand, questions);
        log.info("세션 저장 결과: {}", rDTO);

        // then
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.questions()).hasSize(1);
        assertThat(rDTO.sessionTitle()).isEqualTo("백엔드 개발자 모의면접");
    }

    @Test
    @Transactional
    @DisplayName("세션 저장 성공 - 카테고리 3개 저장 확인")
    void saveSession_success_multipleCategories() throws Exception {
        // given
        List<String> categoryNames = List.of("백엔드 개발자", "자료구조/알고리즘", "운영체제");

        List<AiQuestionItemDTO> questions = List.of(
                AiQuestionItemDTO.builder()
                        .order(1).category("백엔드 개발자")
                        .difficulty("MEDIUM").content("테스트 질문 1번")
                        .intent("출제 의도 1").keywords(List.of("키워드1", "키워드2"))
                        .build(),
                AiQuestionItemDTO.builder()
                        .order(2).category("자료구조/알고리즘")
                        .difficulty("HARD").content("테스트 질문 2번")
                        .intent("출제 의도 2").keywords(List.of("키워드3", "키워드4"))
                        .build(),
                AiQuestionItemDTO.builder()
                        .order(3).category("운영체제")
                        .difficulty("EASY").content("테스트 질문 3번")
                        .intent("출제 의도 3").keywords(List.of("키워드5", "키워드6"))
                        .build()
        );

        SessionCreateCommand pCommand = SessionCreateCommand.builder()
                .userId(1L)
                .sessionTitle("백엔드 개발자 · 자료구조/알고리즘 · 운영체제 모의면접")
                .questionCount(3)
                .selectedCategories(List.of(                          // ← 추가
                        CategoryItemRequestDTO.builder()
                                .categoryId(101L).categoryType("JOB").build(),
                        CategoryItemRequestDTO.builder()
                                .categoryId(10L).categoryType("COMMON_CS").build(),
                        CategoryItemRequestDTO.builder()
                                .categoryId(12L).categoryType("COMMON_CS").build()
                ))
                .categoryNames(categoryNames)
                .build();

        // when
        SessionCreateResponseDTO rDTO = sessionSaveService.saveSession(pCommand, questions);
        log.info("세션 저장 결과: {}", rDTO);

        // then
        assertThat(rDTO.sessionId()).isNotNull();
        assertThat(rDTO.questions()).hasSize(3);
        assertThat(rDTO.sessionTitle()).contains("백엔드 개발자");
    }
}