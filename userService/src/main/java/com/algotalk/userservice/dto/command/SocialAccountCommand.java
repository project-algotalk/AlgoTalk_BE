package com.algotalk.userservice.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Service 계층에서 Controller로부터 전달받은 소셜 계정 정보를 담는 DTO
 *
 * 소셜 로그인/회원가입 시 필요한 정보를 담을 수 있도록 설계
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccountCommand {
    private String provider;      // 소셜 제공자 (예: "KAKAO", "GOOGLE")
    private String providerId;    // 소셜 제공자에서 발급한 고유 ID
    private String email;         // 소셜 계정 이메일 (암호화한 이메일)
    private String name;          // 소셜 계정 이름
}
