package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record EmploymentRequestDTO(
        @NotNull(message = "직무 카테고리를 선택해주세요.")
        Long categoryId,

        String categoryName,

        @NotBlank(message = "회사명을 입력해주세요.")
        String companyName,

        @NotBlank(message = "입사일을 입력해주세요.")
        String startDate,

        // 종료일 null = 현재 재직중
        String endDate
) {
}
