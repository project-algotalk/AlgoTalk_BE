package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.AdminUserCommand;
import com.algotalk.userservice.dto.request.AdminUserListRequestDTO;
import com.algotalk.userservice.dto.response.AdminUserPageResponseDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IAdminUserMapper;
import com.algotalk.userservice.service.ILoginAttemptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
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
    @DisplayName("일반 회원 목록 조회 성공 - 잠금 상태 포함")
    void getUsers_success_withLockStatus() throws Exception {
        // given
        AdminUserCommand lockedUser = AdminUserCommand.builder()
                .userId(10L)
                .loginId("user01")
                .nickname("테스터1")
                .name("테스트1")
                .createdAt(LocalDateTime.of(2026, 9, 25, 10, 0))
                .build();
        AdminUserCommand unlockedUser = AdminUserCommand.builder()
                .userId(11L)
                .loginId("user02")
                .nickname("테스터2")
                .name("테스트2")
                .createdAt(LocalDateTime.of(2026, 9, 24, 10, 0))
                .build();

        given(adminUserMapper.getUsers(argThat(command ->
                "user".equals(command.getKeyword())
                        && command.getPagination().getPage() == 1
                        && command.getPagination().getSize() == 10
        ))).willReturn(List.of(lockedUser, unlockedUser));
        given(adminUserMapper.countUsers(argThat(command ->
                "user".equals(command.getKeyword())
        ))).willReturn(11L);
        given(loginAttemptService.getLockStatuses(
                List.of("user01", "user02")
        )).willReturn(Map.of(
                "user01", true,
                "user02", false
        ));

        // when
        AdminUserPageResponseDTO result = adminUserService.getUsers(
                new AdminUserListRequestDTO(1, 10, " user ")
        );

        // then
        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).loginId()).isEqualTo("user01");
        assertThat(result.content().get(0).locked()).isTrue();
        assertThat(result.content().get(1).locked()).isFalse();
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalCount()).isEqualTo(11L);
        assertThat(result.totalPages()).isEqualTo(2);
    }

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

        given(adminUserMapper.getUserByLoginId("user01")).willReturn(user);

        // when
        adminUserService.unlockUser(" user01 ", 1L);

        // then
        verify(loginAttemptService).resetAttempts("user01");
    }

    @Test
    @DisplayName("회원 계정 잠금 해제 실패 - 회원 정보 없음")
    void unlockUser_fail_userNotFound() throws Exception {
        // given
        given(adminUserMapper.getUserByLoginId("unknownUser")).willReturn(null);

        // when, then
        assertThatThrownBy(() -> adminUserService.unlockUser("unknownUser", 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException exception = (BusinessException) error;
                    assertThat(exception.getErrorCode())
                            .isEqualTo(UserErrorCode.ADMIN_USER_NOT_FOUND);
                });

        verifyNoInteractions(loginAttemptService);
    }
}
