package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.AdminUserListRequestDTO;
import com.algotalk.userservice.dto.response.AdminUserPageResponseDTO;

public interface IAdminUserService {

    AdminUserPageResponseDTO getUsers(
            AdminUserListRequestDTO request
    ) throws Exception;

    // 회원 계정 잠금 해제
    void unlockUser(String loginId, Long adminUserId) throws Exception;
}
