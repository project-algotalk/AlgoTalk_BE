package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TargetJobRequestDTO(
        @NotNull(message = "직무 카테고리를 선택해주세요.")
        Long categoryId,

        String categoryName,

        @NotBlank(message = "준비 시작일을 입력해주세요.")
        String startDate,

        // 종료일 null = 현재 준비중
        String endDate
) {
}
