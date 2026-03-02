package com.algotalk.userservice.auth;

import lombok.Builder;

import java.util.List;

@Builder
public record RtSession(
    String userId,
    String nickname,
    List<String> roles, // ["ROLE_ADMIN", "ROLE_USER"]
    String uaHash,      // User-Agent 해시값(기기 변경 감지용)
    String issuedAt    // 발급 시간
) {
}
