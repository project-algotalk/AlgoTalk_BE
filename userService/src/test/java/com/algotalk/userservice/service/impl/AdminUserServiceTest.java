package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.AdminUserCommand;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IAdminUserMapper;
import com.algotalk.userservice.service.ILoginAttemptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @InjectMocks
    private AdminUserService adminUserService;

    @Mock
    private IAdminUserMapper adminUserMapper;

    @Mock
    private ILoginAttemptService loginAttemptService;

    @Test
    @DisplayName("회원 계정 잠금 해제 성공")
    void unlockUser_success() throws Exception {
        // given
        AdminUserCommand user = AdminUserCommand.builder()
                .userId(10L)
                .loginId("user01")
                .nickname("테스터")
                .name("테스트")
                .build();

        given(adminUserMapper.getUserById(10L)).willReturn(user);

        // when
        adminUserService.unlockUser(10L, 1L);

        // then
        verify(loginAttemptService).resetAttempts("user01");
    }

    @Test
    @DisplayName("회원 계정 잠금 해제 실패 - 회원 정보 없음")
    void unlockUser_fail_userNotFound() throws Exception {
        // given
        given(adminUserMapper.getUserById(999L)).willReturn(null);

        // when, then
        assertThatThrownBy(() -> adminUserService.unlockUser(999L, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(UserErrorCode.ADMIN_USER_NOT_FOUND);
                });

        verifyNoInteractions(loginAttemptService);
    }
}