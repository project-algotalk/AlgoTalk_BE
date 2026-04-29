package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.UpdatePasswordRequestDTO;


public interface IUserUpdateService {
    // 로그인 정보 변경
    // 비밀번호 변경
    int updatePassword(Long userId, UpdatePasswordRequestDTO pDTO) throws Exception;
}
