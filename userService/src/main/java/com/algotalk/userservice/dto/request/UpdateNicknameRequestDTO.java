package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateNicknameRequestDTO(
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣]*$", message = "닉네임은 영문, 숫자, 한글만 사용 가능합니다.")
        String nickname
) {
}
