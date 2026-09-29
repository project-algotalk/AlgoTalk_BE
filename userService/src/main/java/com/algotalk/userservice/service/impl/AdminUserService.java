package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.common.pagination.Pagination;
import com.algotalk.userservice.dto.command.AdminUserCommand;
import com.algotalk.userservice.dto.request.AdminUserListRequestDTO;
import com.algotalk.userservice.dto.response.AdminUserPageResponseDTO;
import com.algotalk.userservice.dto.response.AdminUserResponseDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IAdminUserMapper;
import com.algotalk.userservice.service.IAdminUserService;
import com.algotalk.userservice.service.ILoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService implements IAdminUserService {

    private final IAdminUserMapper adminUserMapper;

    private final ILoginAttemptService loginAttemptService;

    @Override
    @Transactional(readOnly = true)
    public AdminUserPageResponseDTO getUsers(
            AdminUserListRequestDTO request
    ) throws Exception {
        Pagination pagination = request.toPagination();
        AdminUserCommand command = AdminUserCommand.builder()
                .keyword(request.normalizedKeyword())
                .pagination(pagination)
                .build();

        List<AdminUserCommand> users = adminUserMapper.getUsers(command);
        long totalCount = adminUserMapper.countUsers(command);
        Map<String, Boolean> lockStatuses = loginAttemptService
                .getLockStatuses(
                        users.stream()
                                .map(AdminUserCommand::getLoginId)
                                .toList()
                );

        List<AdminUserResponseDTO> content = users.stream()
                .map(user -> new AdminUserResponseDTO(
                        user.getLoginId(),
                        user.getNickname(),
                        user.getName(),
                        user.getCreatedAt(),
                        lockStatuses.getOrDefault(
                                user.getLoginId(),
                                false
                        )
                ))
                .toList();

        int totalPages = (int) Math.ceil(
                (double) totalCount / pagination.getSize()
        );

        return new AdminUserPageResponseDTO(
                content,
                pagination.getPage(),
                pagination.getSize(),
                totalCount,
                totalPages
        );
    }

    @Override
    public void unlockUser(String loginId, Long adminUserId)
            throws Exception {

        String normalizedLoginId = loginId.trim();
        AdminUserCommand user = adminUserMapper.getUserByLoginId(
                normalizedLoginId
        );

        if (user == null) {
            throw new BusinessException(UserErrorCode.ADMIN_USER_NOT_FOUND);
        }

        loginAttemptService.resetAttempts(user.getLoginId());

        log.info(
                "회원 계정 잠금 해제: adminUserId={}, targetUserId={}, loginId={}",
                adminUserId,
                user.getUserId(),
                user.getLoginId()
        );
    }
}
