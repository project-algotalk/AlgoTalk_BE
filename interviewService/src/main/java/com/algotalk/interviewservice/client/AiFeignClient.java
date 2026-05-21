package com.algotalk.interviewservice.client;

import com.algotalk.interviewservice.dto.request.AiQuestionRequestDTO;
import com.algotalk.interviewservice.dto.response.AiQuestionResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "aiService", url = "${ai.service.url}")
public interface AiFeignClient {

    // aiService 면접 질문 생성 요청
    @PostMapping("/ai/v1/interview")
    AiQuestionResponseDTO generateQuestions(@RequestBody AiQuestionRequestDTO request);
}
