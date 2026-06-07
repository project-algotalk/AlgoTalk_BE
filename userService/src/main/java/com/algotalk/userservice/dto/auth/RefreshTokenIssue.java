package com.algotalk.userservice.dto.auth;

import java.time.Instant;

/**
 * Refresh Token 발급 결과
 * token: JWT Refresh Token
 * sessionId: 로그인 세션 식별자
 * expiresAt: sliding 만료와 절대 만료 중 더 이른 최종 RT 만료 시각
 * absoluteExpiresAt: 최초 로그인 시 고정되는 세션 절대 만료 시각
 */
public record RefreshTokenIssue(
        String token,
        String sessionId,
        Instant expiresAt,
        Instant absoluteExpiresAt
) {
}