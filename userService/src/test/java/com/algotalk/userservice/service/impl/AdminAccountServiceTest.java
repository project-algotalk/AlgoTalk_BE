package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.dto.command.AdminAccountCommand;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.AdminSignUpRequestDTO;
import com.algotalk.userservice.repository.IAdminAccountMapper;
import com.algotalk.userservice.repository.IUserRegMapper;
import com.algotalk.userservice.service.IRefreshTokenService;
import com.algotalk.userservice.service.IUserRegService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {
    @Mock
    IUserRegService userRegService;
    @Mock
    IUserRegMapper userRegMapper;
    @Mock
    IAdminAccountMapper adminAccountMapper;
    @Mock
    IRefreshTokenService refreshTokenService;
    @Mock
    PasswordEncoder passwordEncoder;
    @InjectMocks
    AdminAccountService service;

    @DisplayName("관리자 가입 신청 시, ROLE_ADMIN_PENDING 역할이 부여되고 승인 상태는 PENDING으로 설정")
    @Test
    void signUpCreatesPendingAdmin() throws Exception {
        AdminSignUpRequestDTO request = new AdminSignUpRequestDTO(
                "admin01", "Password1!", "Password1!", "관리자", "관리자01");
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded");
        when(userRegMapper.insertUser(any())).thenAnswer(invocation -> {
            UserInfoCommand user = invocation.getArgument(0);
            var field = UserInfoCommand.class.getDeclaredField("userId");
            field.setAccessible(true);
            field.set(user, 10L);
            return 1;
        });
        when(userRegMapper.insertUserCredential(any())).thenReturn(1);
        when(userRegMapper.insertUserRoles(any())).thenReturn(1);
        when(adminAccountMapper.insertAdminAccount(any())).thenReturn(1);

        var result = service.signUp(request);

        ArgumentCaptor<UserInfoCommand> userCaptor = ArgumentCaptor.forClass(UserInfoCommand.class);
        verify(userRegMapper).insertUserRoles(userCaptor.capture());
        assertThat(userCaptor.getValue().getRole()).isEqualTo("ROLE_ADMIN_PENDING");
        assertThat(result.approvalStatus()).isEqualTo("PENDING");
    }

    @DisplayName("관리자 계정 승인 시, ROLE_ADMIN으로 변경되고 모든 세션이 만료됨")
    @Test
    void approveChangesRoleAndRevokesSessions() throws Exception {
        when(adminAccountMapper.getAdminAccount(any(AdminAccountCommand.class))).thenReturn(
                AdminAccountCommand.builder().userId(10L).approvalStatus("PENDING").build());
        when(adminAccountMapper.approveAdmin(any())).thenReturn(1);
        when(adminAccountMapper.updateAdminRole(any())).thenReturn(1);

        service.approve(10L, 1L);

        ArgumentCaptor<AdminAccountCommand> captor = ArgumentCaptor.forClass(AdminAccountCommand.class);
        verify(adminAccountMapper).updateAdminRole(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("ROLE_ADMIN");
        verify(refreshTokenService).deleteAllRefreshTokens(10L);
    }
}
