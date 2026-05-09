package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record SetPasswordRequestDTO(
        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 하며 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\W_]).+$", message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
        String newPasswordConfirm
) {
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(newPasswordConfirm);
    }
}
