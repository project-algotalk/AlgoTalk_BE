package com.algotalk.interviewservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record ManualSessionCreateRequestDTO(
        @NotEmpty(message = "질문을 최소 1개 입력해주세요.")
        @Size(max = 5, message = "질문은 최대 5개까지 입력 가능합니다.")
        @Valid
        List<ManualQuestionItemRequestDTO> questions       // 질문 목록 (카테고리 + 질문 텍스트)
) {}