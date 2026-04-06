package com.algotalk.userservice.service;

public interface IRefreshTokenService {

    /**
     * Refresh Token 저장
     * @param userId
     * @param refreshToken
     * @throws Exception
     */
    void saveRefreshToken(Long userId, String refreshToken) throws Exception;

    /**
     * userId로 refresh Token 조회
     * @param userId
     * @return
     * @throws Exception
     */
    String getRefreshToken(Long userId) throws Exception;

    /**
     * userId로 refresh Token 삭제
     * @param userId
     * @throws Exception
     */
    void deleteRefreshToken(Long userId) throws Exception;

    /**
     * userId와 refresh Token 일치 여부 검증
     * @param userId
     * @param refreshToken
     * @return
     * @throws Exception
     */
    boolean validateRefreshToken(Long userId, String refreshToken) throws Exception;

}
