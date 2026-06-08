package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.response.UserInfoResponseDTO;

public interface IUserInfoService {

    // 사용자 닉네임 조회
    UserInfoResponseDTO getNicknameByUserId(UserInfoCommand pCommand);
}
