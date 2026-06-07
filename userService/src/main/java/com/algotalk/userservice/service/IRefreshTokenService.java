package com.algotalk.userservice.service;

public interface IRefreshTokenService {

    /**
     * Refresh Token 저장
     *
     * @param userId Refresh Token 소유 사용자 ID
     * @param refreshToken 저장할 Refresh Token
     * @throws Exception Refresh Token 저장 중 오류가 발생한 경우
     */
    void saveRefreshToken(Long userId, String refreshToken) throws Exception;

    /**
     * userId로 Refresh Token 조회
     *
     * @param userId Refresh Token 소유 사용자 ID
     * @return Redis에 저장된 Refresh Token, 존재하지 않으면 null
     * @throws Exception Refresh Token 조회 중 오류가 발생한 경우
     */
    String getRefreshToken(Long userId) throws Exception;

    /**
     * userId로 Refresh Token 삭제
     *
     * @param userId Refresh Token을 삭제할 사용자 ID
     * @throws Exception Refresh Token 삭제 중 오류가 발생한 경우
     */
    void deleteRefreshToken(Long userId) throws Exception;

    /**
     * userId와 Refresh Token 일치 여부 검증
     *
     * @param userId Refresh Token 소유 사용자 ID
     * @param refreshToken 검증할 Refresh Token
     * @return Redis에 저장된 Refresh Token과 일치하면 true, 아니면 false
     * @throws Exception Refresh Token 검증 중 오류가 발생한 경우
     */
    boolean validateRefreshToken(Long userId, String refreshToken) throws Exception;

    /**
     * Refresh Token 교체(RTR)
     *
     * Redis에 저장된 RT가 expectedRefreshToken과 같을 때에만 새 RT로 교체
     * 기존 RT 비교, 새 RT 저장, TTL 설정은 Redis Lua script를 통해
     * 하나의 원자적 연산으로 실행
     *
     * @param userId Refresh Token 소유 사용자 ID
     * @param expectedRefreshToken 요청이 제출한 기존 Refresh Token
     * @param newRefreshToken 새로 발급한 Refresh Token
     * @return Refresh Token 교체 결과
     * @throws Exception Refresh Token 교체 중 오류가 발생한 경우
     */
    RotationResult rotateRefreshToken(
            Long userId,
            String expectedRefreshToken,
            String newRefreshToken
    ) throws Exception;

    /** 원자적 RTR 결과를 재발급 서비스가 오류 코드로 변환할 수 있게 구분함 */
    enum RotationResult {
        // 새 Refresh Token으로 정상 교체됨
        ROTATED,

        // Redis에서 해당 사용자의 Refresh Token을 찾지 못함
        NOT_FOUND,

        // Redis RT와 요청이 제출한 기존 RT가 일치하지 않음
        MISMATCH
    }
}