package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record EmailVerifyRequestDTO(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,

        /**
         * DB 테이블에 존재하지 않는 가상의 컬럼
         * 회원 가입시 인증번호 발송 후 검증 용으로 사용
         */
        String authNumber
) {
}
