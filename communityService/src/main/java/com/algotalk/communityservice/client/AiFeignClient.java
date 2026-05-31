package com.algotalk.communityservice.client;

import com.algotalk.communityservice.dto.request.CsValidationRequestDTO;
import com.algotalk.communityservice.dto.response.CsValidationResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "aiService", url = "${ai.service.url}")
public interface AiFeignClient {

    // aiService CS 질문 검증 요청
    @PostMapping("/ai/v1/validate/cs-questions")
    CsValidationResponseDTO validateCsQuestions(@RequestBody CsValidationRequestDTO request);

}
