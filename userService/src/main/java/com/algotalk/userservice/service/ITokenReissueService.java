package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.response.TokenReissueResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface ITokenReissueService {
    /**
     * Refresh Token을 가지고 Access Token 재발급
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    TokenReissueResponseDTO reissueToken(HttpServletRequest request, HttpServletResponse response) throws Exception;
}
