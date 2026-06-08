package com.algotalk.userservice.service;

import com.algotalk.userservice.domain.enums.RefreshTokenRotationResult;

import java.time.Instant;

public interface IRefreshTokenService {


    /**
     * 로그인 세션별 Refresh Token 저장
     *
     * @param userId Refresh Token 소유 사용자 ID
     * @param sessionId 로그인 세션 식별자
     * @param refreshToken 저장할 Refresh Token
     * @param expiresAt Refresh Token의 최종 만료 시각
     * @throws Exception Refresh Token 저장 실패
     */
    void saveRefreshToken(
            Long userId,
            String sessionId,
            String refreshToken,
            Instant expiresAt
    ) throws Exception;

    /**
     * 로그인 세션별 Refresh Token 조회
     *
     * @param userId Refresh Token 소유 사용자 ID
     * @param sessionId 로그인 세션 식별자
     * @return 저장된 Refresh Token, 존재하지 않는 경우 null
     * @throws Exception Refresh Token 조회 실패
     */
    String getRefreshToken(Long userId, String sessionId) throws Exception;

    /**
     * 현재 로그인 세션의 Refresh Token 삭제
     *
     * @param userId Refresh Token 소유 사용자 ID
     * @param sessionId 삭제할 로그인 세션 식별자
     * @throws Exception Refresh Token 삭제 실패
     */
    void deleteRefreshToken(Long userId, String sessionId) throws Exception;

    /**
     * 사용자의 모든 Refresh Token 세션 삭제
     *
     * 전체 로그아웃과 회원 탈퇴 시 사용
     *
     * @param userId 전체 세션을 삭제할 사용자 ID
     * @throws Exception Refresh Token 세션 삭제 실패
     */
    void deleteAllRefreshTokens(Long userId) throws Exception;

    /**
     * 로그인 세션별 Refresh Token 일치 여부 검증
     *
     * @param userId Refresh Token 소유 사용자 ID
     * @param sessionId 로그인 세션 식별자
     * @param refreshToken 검증할 Refresh Token
     * @return Redis 저장값과 일치하는 경우 true, 그 외 false
     * @throws Exception Refresh Token 검증 실패
     */
    boolean validateRefreshToken(Long userId, String sessionId, String refreshToken) throws Exception;

    /**
     * 로그인 세션별 Refresh Token 원자적 회전
     *
     * Redis 저장값이 expectedRefreshToken과 일치하는 경우에만 새 토큰과 TTL로 교체
     *
     * @param userId Refresh Token 소유 사용자 ID
     * @param sessionId 로그인 세션 식별자
     * @param expectedRefreshToken 요청에서 제출된 기존 Refresh Token
     * @param newRefreshToken 새로 발급된 Refresh Token
     * @param expiresAt 새 Refresh Token의 최종 만료 시각
     * @return Redis 원자적 교체 결과
     * @throws Exception Refresh Token 교체 실패
     */
    RefreshTokenRotationResult rotateRefreshToken(
            Long userId,
            String sessionId,
            String expectedRefreshToken,
            String newRefreshToken,
            Instant expiresAt
    ) throws Exception;
}