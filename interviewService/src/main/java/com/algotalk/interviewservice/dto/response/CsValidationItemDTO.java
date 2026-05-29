package com.algotalk.interviewservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CsValidationItemDTO(
        String questionText,  // 질문 내용
        boolean isValid,      // CS 관련 여부
        String reason         // 판단 이유
) {}