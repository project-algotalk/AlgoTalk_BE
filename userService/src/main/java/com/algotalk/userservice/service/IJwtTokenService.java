package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.command.UserInfoCommand;

public interface IJwtTokenService {
    /**
     * ACCESS TOKEN 생성
     * @param pCommand
     * @return
     * @throws Exception
     */
    String generateAccessToken(UserInfoCommand pCommand) throws Exception;

    /**
     * REFRESH TOKEN 생성
     * @param pCommand
     * @return
     * @throws Exception
     */
    String generateRefreshToken(UserInfoCommand pCommand) throws Exception;

    /**
     * ACCESS TOKEN에서 사용자 ID 추출
     * @param token
     * @return
     * @throws Exception
     */
    Long getUserIdFromToken(String token) throws Exception;
}
