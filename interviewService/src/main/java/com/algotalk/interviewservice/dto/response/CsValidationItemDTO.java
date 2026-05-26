package com.algotalk.interviewservice.dto.response;

import lombok.Builder;

@Builder
public record CsValidationItemDTO(
        String questionText,  // 질문 내용
        boolean isValid,      // CS 관련 여부
        String reason         // 판단 이유
) {}