package com.algotalk.interviewservice.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum CsCategoryType {
    COMMON_CS("COMMON","COMMON_CS", "직무 공통"),
    JOB("JOB_SPECIFIC","JOB", "직무 특화");

    private final String categoryCd;
    private final String categoryType;
    private final String categoryName;

    // 카테고리 타입 검증
    public static boolean isValid(String categoryType) {
        return Arrays.stream(values())
                .anyMatch(value -> value.getCategoryType().equals(categoryType));
    }
}