package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.ToString;

import java.util.List;

@Builder
public record SignUpRequestDTO(

        // TB: USER_CREDENTIAL
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하여야 합니다.")
        @Pattern(regexp = "^[a-zA-Z0-9]*$", message = "아이디는 영문, 숫자만 사용 가능합니다.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, message = "비밀번호는 8자 이상이어야 하며 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[\\W_]).+$", message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
        String password,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String passwordConfirm,

        // TB: USER
//        @NotBlank(message = "이메일을 입력해주세요.") // 선택 입력(소셜 로그인 고려)
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        String addr1,
        String addr2,

        @NotBlank(message = "이름을 입력해주세요.")
        String name,

        String nickname,

        // 목표 직무(최대 3개)
        @Size(max = 3, message = "목표 직무는 최대 3개까지 입력 가능합니다.")
        List<TargetJobRequestDTO> targetJobs,

        // 경력 정보
        List<EmploymentRequestDTO> employments
) {
    // 닉네임 입력안하면 이름으로 대체
    public String resolvedNickname() {
        return (nickname == null || nickname.isBlank()) ? name : nickname;
    }

    // 비밀번호 일치 검사
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(passwordConfirm);
    }
}
