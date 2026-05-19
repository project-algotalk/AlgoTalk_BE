package com.algotalk.interviewservice.dto.request;

import jakarta.validation.constraints.NotNull;

public record CategoryItemRequestDTO(

        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,   // CS_CATEGORY 카테고리 ID

        @NotNull(message = "카테고리 타입은 필수입니다.")
        String categoryType        // 카테고리 구분 (COMMON_CS: 직무 공통 / JOB: 직무 특화)

) {}