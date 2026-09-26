package com.algotalk.userservice.service;

public interface IAdminUserService {

    // 회원 계정 잠금 해제
    void unlockUser(Long targetUserId, Long adminUserId) throws Exception;
}