package com.algotalk.userservice.auth;

import lombok.Builder;

import java.util.List;

/**
 * Refresh Token 세션 정보
 * @param userId
 * @param nickname
 * @param roles
 * @param handle
 * @param uaHash
 * @param issuedAt
 */
@Builder
public record RtSession(
    Long userId,
    String nickname,
    List<String> roles, // ["ROLE_ADMIN", "ROLE_USER"]
    String handle,      // 로그인 핸들(소셜 로그인 시 구분자)
    String uaHash,      // User-Agent 해시값(기기 변경 감지용)
    String issuedAt    // 발급 시간
) {
}
