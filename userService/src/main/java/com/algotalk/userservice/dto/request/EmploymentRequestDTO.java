package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record EmploymentRequestDTO(
        @NotNull(message = "직무 카테고리를 선택해주세요.")
        Long categoryId,

        @NotBlank(message = "회사명을 입력해주세요.")
        String companyName,

        @NotNull(message = "입사일을 입력해주세요.")
        LocalDate startDate,

        // 종료일 null = 현재 재직중
        LocalDate endDate
) {
}
