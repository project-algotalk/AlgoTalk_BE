package com.algotalk.userservice.domain.enums;

/** Refresh Token 원자적 회전 결과 */
public enum RefreshTokenRotationResult {
    // 새 Refresh Token과 TTL로 교체 완료
    ROTATED,

    // 대상 로그인 세션의 Redis key 부재
    NOT_FOUND,

    // Redis 저장값과 요청의 기존 Refresh Token 불일치
    MISMATCH
}