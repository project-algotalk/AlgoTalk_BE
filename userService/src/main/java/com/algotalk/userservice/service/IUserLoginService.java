package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.dto.response.LoginResponseDTO;
import jakarta.servlet.http.HttpServletResponse;

public interface IUserLoginService {

    /**
     * 로그인
     * @param pDTO
     * @param response
     * @return
     * @throws Exception
     */
    LoginResponseDTO login(LoginRequestDTO pDTO, HttpServletResponse response) throws Exception;

    /**
     * 로그아웃
     * @param userId
     * @param response
     * @throws Exception
     */
    void logout(Long userId, HttpServletResponse response) throws Exception;
}
