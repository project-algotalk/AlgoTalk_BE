package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.WithdrawRequestDTO;

public interface IUserWithdrawService {
    // 소셜 계졍 연동 해제
    int unlinkSocial(Long userId, String provider) throws Exception;

    // 회원 탈퇴
    int withdraw(Long userId, WithdrawRequestDTO pDTO) throws Exception;
}
