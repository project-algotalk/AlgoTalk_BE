package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.AdminUserCommand;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IAdminUserMapper;
import com.algotalk.userservice.service.IAdminUserService;
import com.algotalk.userservice.service.ILoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService implements IAdminUserService {

    private final IAdminUserMapper adminUserMapper;

    private final ILoginAttemptService
            loginAttemptService;

    @Override
    public void unlockUser(Long targetUserId, Long adminUserId)
            throws Exception {

        AdminUserCommand user = adminUserMapper.getUserById(targetUserId);

        if (user == null) {
            throw new BusinessException(UserErrorCode.ADMIN_USER_NOT_FOUND);
        }

        loginAttemptService.resetAttempts(user.getLoginId());

        log.info(
                "회원 계정 잠금 해제: adminUserId={}, targetUserId={}, loginId={}",
                adminUserId,
                targetUserId,
                user.getLoginId()
        );
    }
}