package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record LoginIdCheckRequestDTO(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "아이디는 영문, 숫자만 사용 가능합니다.")
        String loginId
) {
}
