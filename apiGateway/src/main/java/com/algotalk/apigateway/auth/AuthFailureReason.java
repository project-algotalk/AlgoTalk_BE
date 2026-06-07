package com.algotalk.apigateway.auth;

/**
 * JWT 인증 실패 원인을 필터 사이에서 전달하기 위한 값
 *
 * AutoRefreshOn401Filter는 단순히 HTTP 401 여부만 확인하지 않고,
 * 이 값이 ACCESS_TOKEN_EXPIRED일 때에만 RT 재발급을 시도함으로써,
 * 불필요한 RT 재발급과 그에 따른 부하를 줄이는 효과가 있음
 */
public enum AuthFailureReason {
    // 정상적으로 발급된 Access Token이지만 exp가 지나 만료된 경우
    ACCESS_TOKEN_EXPIRED,

    // 서명 오류, 형식 오류 등 재발급으로 복구하면 안 되는 경우
    ACCESS_TOKEN_INVALID
}