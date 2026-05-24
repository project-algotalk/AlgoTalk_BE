package com.algotalk.interviewservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record ManualQuestionItemRequestDTO(
        @NotNull(message = "카테고리를 선택해주세요.")
        Long categoryId,                                   // 카테고리 ID

        @NotBlank(message = "질문 내용은 공백일 수 없습니다.")
        String questionText                                // 질문 내용
) {}