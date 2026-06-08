package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record UpdateNameRequestDTO (
        @NotBlank(message = "이름을 입력해주세요.")
        String name
) {
}
