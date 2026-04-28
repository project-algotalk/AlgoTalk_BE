package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.FindLoginIdRequestDTO;
import com.algotalk.userservice.dto.request.FindPasswordRequestDTO;
import com.algotalk.userservice.dto.request.ResetPasswordRequestDTO;
import com.algotalk.userservice.dto.response.UserInfoResponseDTO;

public interface IFindAccountService {
    // 아이디 찾기 - 이메일 인증번호 발송
    void sendFindLoginIdEmail(FindLoginIdRequestDTO pDTO) throws Exception;
    // 아이디 찾기 - 아이디 조회
    UserInfoResponseDTO findLoginId(FindLoginIdRequestDTO pDTO) throws Exception;
    // 비밀번호 찾기 - 이메일 인증번호 발송
    void sendFindPasswordEmail(FindPasswordRequestDTO pDTO) throws Exception;
    // 비밀번호 찾기 - 비밀번호 재설정
    void resetPassword(ResetPasswordRequestDTO pDTO) throws Exception;
}
