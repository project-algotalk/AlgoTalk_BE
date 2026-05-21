package com.algotalk.interviewservice.client;

import com.algotalk.interviewservice.dto.request.AiQuestionRequestDTO;
import com.algotalk.interviewservice.dto.request.CsValidationRequestDTO;
import com.algotalk.interviewservice.dto.response.AiQuestionResponseDTO;
import com.algotalk.interviewservice.dto.response.CsValidationResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "aiService", url = "${ai.service.url}")
public interface AiFeignClient {

    // aiService 면접 질문 생성 요청
    @PostMapping("/ai/v1/interview/questions")
    AiQuestionResponseDTO generateQuestions(@RequestBody AiQuestionRequestDTO request);

    // aiService CS 질문 검증 요청
    @PostMapping("/ai/v1/validate/cs-questions")
    CsValidationResponseDTO validateCsQuestions(@RequestBody CsValidationRequestDTO request);

}
