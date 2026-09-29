package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.AdminAccountCommand;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.AdminApplicationListRequestDTO;
import com.algotalk.userservice.dto.request.AdminSignUpRequestDTO;
import com.algotalk.userservice.dto.request.CheckLoginIdRequestDTO;
import com.algotalk.userservice.dto.request.CheckNicknameRequestDTO;
import com.algotalk.userservice.dto.response.AdminApplicationResponseDTO;
import com.algotalk.userservice.dto.response.AdminSignUpResponseDTO;
import com.algotalk.userservice.dto.response.AdminStatusResponseDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IAdminAccountMapper;
import com.algotalk.userservice.repository.IUserRegMapper;
import com.algotalk.userservice.service.IAdminAccountService;
import com.algotalk.userservice.service.IRefreshTokenService;
import com.algotalk.userservice.service.IUserRegService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAccountService implements IAdminAccountService {
    private final IUserRegService userRegService;
    private final IUserRegMapper userRegMapper;
    private final IAdminAccountMapper adminAccountMapper;
    private final IRefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public AdminSignUpResponseDTO signUp(AdminSignUpRequestDTO request) throws Exception {
        if (!request.isPasswordConfirmed()) {
            throw new BusinessException(UserErrorCode.PASSWORD_MISMATCH);
        }
        userRegService.validateLoginIdUnique(new CheckLoginIdRequestDTO(request.loginId()));
        userRegService.validateNicknameUnique(new CheckNicknameRequestDTO(request.nickname()));

        UserInfoCommand user = UserInfoCommand.builder()
                .loginId(request.loginId().strip())
                .password(passwordEncoder.encode(request.password()))
                .passwordSetYn("Y")
                .nickname(request.nickname().strip())
                .name(request.name().strip())
                .role("ROLE_ADMIN_PENDING")
                .build();
        if (userRegMapper.insertUser(user) != 1
                || userRegMapper.insertUserCredential(user) != 1
                || userRegMapper.insertUserRoles(user) != 1
                || adminAccountMapper.insertAdminAccount(AdminAccountCommand.builder().userId(user.getUserId()).build()) != 1) {
            throw new BusinessException(UserErrorCode.ADMIN_SIGN_UP_FAIL);
        }
        return new AdminSignUpResponseDTO(user.getUserId(), "PENDING");
    }

    @Override
    public AdminStatusResponseDTO getStatus(Long userId) throws Exception {
        AdminAccountCommand pCommand = requireAccount(userId);
        return new AdminStatusResponseDTO(pCommand.getUserId(), pCommand.getApprovalStatus(),
                pCommand.getAdminGrade(), pCommand.getApprovedAt(), pCommand.getRejectReason());
    }

    @Override
    public List<AdminApplicationResponseDTO> getApplications(AdminApplicationListRequestDTO rDTO) throws Exception {
        AdminAccountCommand pCommand = AdminAccountCommand.builder()
                .approvalStatus(rDTO.status() == null ? null : rDTO.status().name())
                .pagination(rDTO.toPagination()).build();

        return adminAccountMapper.getAdminAccountLists(pCommand).stream()
                .map(row -> new AdminApplicationResponseDTO(row.getUserId()
                        , row.getLoginId()
                        , row.getNickname()
                        , row.getName()
                        , row.getApprovalStatus()
                        , row.getAdminGrade()
                        , row.getCreatedAt()
                        , row.getTotalCount()))
                .toList();
    }

    @Override
    @Transactional
    public void approve(Long targetUserId, Long approverUserId) throws Exception {
        requirePending(targetUserId);
        AdminAccountCommand pCommand = AdminAccountCommand.builder().userId(targetUserId)
                .approvedBy(approverUserId).role("ROLE_ADMIN").build();
        if (adminAccountMapper.approveAdmin(pCommand) != 1 || adminAccountMapper.updateAdminRole(pCommand) != 1) {
            throw new BusinessException(UserErrorCode.ADMIN_APPLICATION_ALREADY_PROCESSED);
        }
        refreshTokenService.deleteAllRefreshTokens(targetUserId);
    }

    @Override
    @Transactional
    public void reject(Long targetUserId, Long approverUserId, String reason) throws Exception {
        requirePending(targetUserId);
        AdminAccountCommand pCommand = AdminAccountCommand.builder().userId(targetUserId)
                .approvedBy(approverUserId).rejectReason(reason).build();
        if (adminAccountMapper.rejectAdmin(pCommand) != 1) {
            throw new BusinessException(UserErrorCode.ADMIN_APPLICATION_ALREADY_PROCESSED);
        }
        refreshTokenService.deleteAllRefreshTokens(targetUserId);
    }

    private AdminAccountCommand requireAccount(Long userId) throws Exception {
        AdminAccountCommand pCommand = AdminAccountCommand.builder()
                .userId(userId).build();
        AdminAccountCommand account = adminAccountMapper.getAdminAccount(pCommand);
        if (account == null) throw new BusinessException(UserErrorCode.ADMIN_APPLICATION_NOT_FOUND);
        return account;
    }

    private void requirePending(Long userId) throws Exception {
        if (!"PENDING".equals(requireAccount(userId).getApprovalStatus())) {
            throw new BusinessException(UserErrorCode.ADMIN_APPLICATION_ALREADY_PROCESSED);
        }
    }
}