package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Builder
public record TargetJobRequestDTO(
        @NotNull(message = "직무 카테고리를 선택해주세요.")
        Long categoryId,

        String categoryName,

        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @NotNull(message = "준비 시작일을 입력해주세요.")
        LocalDate startDate,

        // 종료일 null = 현재 준비중
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate endDate
) {
}
