package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRejectRequestDTO(
        @NotBlank(message = "반려 사유를 입력해주세요.")
        @Size(max = 300, message = "반려 사유는 300자 이하여야 합니다.")
        String reason
) {
}