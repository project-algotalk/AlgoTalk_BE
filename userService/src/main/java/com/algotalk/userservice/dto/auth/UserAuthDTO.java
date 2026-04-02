package com.algotalk.userservice.dto.auth;

import java.util.List;

/**
 * Spring Security 인증 전용 DTO
 * 인증 관련 정보를 담는 DTO
 *
 * CustomUserDetails에서 사용
 * password 포함 -> 외부 응답 절대 사용 하지 않음
 *
 * 기본 로그인 : userId, loginId, password, roles 모두 있음
 * 소셜 로그인 : SocialAuthDTO 별도 사용
 *
 * @param userId
 * @param loginId
 * @param password
 * @param roles
 */
public record UserAuthDTO(
        Long userId,
        String loginId,
        String password,
        List<String> roles
) {
}
