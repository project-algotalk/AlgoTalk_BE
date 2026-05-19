package com.algotalk.interviewservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SessionCreateRequestDTO(

        @NotBlank(message = "세션 제목은 필수입니다.")
        String sessionTitle,           // 세션 제목

        @NotNull(message = "직무 ID는 필수입니다.")
        Long jobId,                    // CS_CATEGORY 직무 ID (DEPTH=2, CATEGORY_TYPE=JOB)

        @NotEmpty(message = "카테고리를 최소 1개 선택해주세요.")
        @Size(max = 3, message = "카테고리는 최대 3개까지 선택 가능합니다.")
        List<CategoryItemRequestDTO> selectedCategories,  // 선택한 카테고리 목록 (최대 3개)

        @Min(value = 1, message = "질문 수는 최소 1개입니다.")
        @Max(value = 5, message = "질문 수는 최대 5개입니다.")
        int questionCount              // 생성할 질문 수 (1~5개)

) {}