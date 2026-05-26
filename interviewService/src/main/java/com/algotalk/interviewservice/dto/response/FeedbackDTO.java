package com.algotalk.interviewservice.dto.response;

import lombok.Builder;

@Builder
public record FeedbackDTO(
        String good,      // 잘한 점
        String improve,   // 부족한 점
        String addition   // 추가할 내용
) {}