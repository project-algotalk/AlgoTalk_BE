package com.algotalk.interviewservice.domain;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GestureDeduction {
    private String type;       // 제스처 유형
    private Integer count;     // 감지 횟수
    private Integer deduction; // 감점
}