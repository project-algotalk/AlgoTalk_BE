package com.algotalk.interviewservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record SessionCreateRequestDTO(
        @NotEmpty(message = "카테고리를 최소 1개 선택해주세요.")
        @Size(max = 3, message = "카테고리는 최대 3개까지 선택 가능합니다.")
        @Valid // 리스트 내부 CategoryItemRequestDTO 객체의 유효성 검사 활성화
        List<CategoryItemRequestDTO> selectedCategories,  // 선택한 카테고리 목록 (최대 3개)
        // COMMON_CS: 직무 공통 / JOB: 직무 특화

        @Min(value = 1, message = "질문 수는 최소 1개입니다.")
        @Max(value = 5, message = "질문 수는 최대 5개입니다.")
        int questionCount                                 // 생성할 질문 수 (1~5개)

) {}