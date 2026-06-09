package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.auth.RefreshTokenIssue;
import com.algotalk.userservice.dto.command.UserInfoCommand;

import java.time.Instant;

public interface IJwtTokenService {
    /**
     * Access Token 생성
     *
     * @param pCommand 토큰 claim 구성에 사용할 사용자 정보
     * @param sessionId Redis 로그인 세션과 연결할 식별자
     * @return 생성된 JWT Access Token
     * @throws Exception Access Token 생성 실패
     */
    String generateAccessToken(UserInfoCommand pCommand, String sessionId) throws Exception;

    /**
     * 새 로그인 세션 생성 및 최초 Refresh Token 발급
     *
     * 로그인마다 새로운 sessionId와 세션 절대 만료 시각 생성
     *
     * @param pCommand 토큰 subject와 claim 구성에 사용할 사용자 정보
     * @return Refresh Token과 세션·만료 정보를 포함한 발급 결과
     * @throws Exception Refresh Token 생성 실패
     */
    RefreshTokenIssue issueRefreshToken(UserInfoCommand pCommand) throws Exception;

    /**
     * 기존 로그인 세션의 Refresh Token 회전
     *
     * 기존 sessionId와 절대 만료 시각 유지 및 새로운 jti 생성
     *
     * @param pCommand 토큰 subject와 claim 구성에 사용할 사용자 정보
     * @param sessionId 기존 로그인 세션 식별자
     * @param absoluteExpiresAt 최초 로그인 시 결정된 세션 절대 만료 시각
     * @return 회전된 Refresh Token과 유지된 세션·만료 정보를 포함한 발급 결과
     * @throws Exception Refresh Token 생성 실패 또는 세션 절대 만료
     */
    RefreshTokenIssue rotateRefreshToken(
            UserInfoCommand pCommand,
            String sessionId,
            Instant absoluteExpiresAt
    ) throws Exception;

    /**
     * JWT subject에서 사용자 ID 추출
     *
     * @param token 사용자 ID를 추출할 JWT
     * @return JWT subject에 저장된 사용자 ID
     * @throws Exception JWT 검증 또는 사용자 ID 변환 실패
     */
    Long getUserIdFromToken(String token) throws Exception;

    /**
     * Refresh Token에서 로그인 세션 식별자 추출
     *
     * @param token sessionId claim을 추출할 Refresh Token
     * @return Refresh Token의 sessionId claim
     * @throws Exception Refresh Token 검증 또는 claim 추출 실패
     */
    String getSessionIdFromToken(String token) throws Exception;

    /**
     * Refresh Token에서 세션 절대 만료 시각 추출
     *
     * @param token sessionExpiresAt claim을 추출할 Refresh Token
     * @return Refresh Token의 세션 절대 만료 시각
     * @throws Exception Refresh Token 검증 또는 claim 변환 실패
     */
    Instant getSessionExpiresAtFromToken(String token) throws Exception;
}
