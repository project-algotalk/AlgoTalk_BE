package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.UpdatePasswordRequestDTO;
import com.algotalk.userservice.repository.IUserRegMapper;
import com.algotalk.userservice.service.IUserUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.algotalk.userservice.exception.UserErrorCode.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UserUpdateServiceTest {

    @Autowired
    private IUserUpdateService userUpdateService;

    @Autowired
    private IUserRegMapper userRegMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email("upd01@algotalk.com")
                .loginId("upd01")
                .password(passwordEncoder.encode("password"))
                .role("USER")
                .build();

        // USERS INSERT (userId 채번)
        userRegMapper.insertUser(cmd);
        assertThat(cmd.getUserId()).isNotNull();

        // USER_CREDENTIAL INSERT
        int credResult = userRegMapper.insertUserCredential(cmd);
        assertThat(credResult).isEqualTo(1);

        // USER_ROLES INSERT
        int roleResult = userRegMapper.insertUserRoles(cmd);
        assertThat(roleResult).isEqualTo(1);

        UpdatePasswordRequestDTO pDTO = UpdatePasswordRequestDTO.builder()
                .currentPassword("password")
                .newPassword("newPassword")
                .newPasswordConfirm("newPassword")
                .build();

        // when
        int res = userUpdateService.updatePassword(cmd.getUserId(), pDTO);

        // then
        assertThat(res).isEqualTo(1);
    }
    
    @Test
    @Transactional
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void updatePassword_fail_currentPwdMissMatch() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email("upd02@algotalk.com")
                .loginId("upd02")
                .password(passwordEncoder.encode("password"))
                .role("USER")
                .build();

        // USERS INSERT (userId 채번)
        userRegMapper.insertUser(cmd);
        assertThat(cmd.getUserId()).isNotNull();

        // USER_CREDENTIAL INSERT
        int credResult = userRegMapper.insertUserCredential(cmd);
        assertThat(credResult).isEqualTo(1);

        // USER_ROLES INSERT
        int roleResult = userRegMapper.insertUserRoles(cmd);
        assertThat(roleResult).isEqualTo(1);

        UpdatePasswordRequestDTO pDTO = UpdatePasswordRequestDTO.builder()
                .currentPassword("notCurrentPassword")
                .newPassword("newPassword")
                .newPasswordConfirm("newPassword")
                .build();

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userUpdateService.updatePassword(cmd.getUserId(), pDTO));
        assertThat(ex.getErrorCode()).isEqualTo(CUR_PASSWORD_MISMATCH);
    }

    @Test
    @Transactional
    @DisplayName("비밀번호 변경 실패 - 새 비밀번호와 확인 비밀번호 불일치")
    void updatePassword_fail_confirmMissMatch() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email("upd03@algotalk.com")
                .loginId("upd03")
                .password(passwordEncoder.encode("password"))
                .role("USER")
                .build();

        // USERS INSERT (userId 채번)
        userRegMapper.insertUser(cmd);
        assertThat(cmd.getUserId()).isNotNull();

        // USER_CREDENTIAL INSERT
        int credResult = userRegMapper.insertUserCredential(cmd);
        assertThat(credResult).isEqualTo(1);

        // USER_ROLES INSERT
        int roleResult = userRegMapper.insertUserRoles(cmd);
        assertThat(roleResult).isEqualTo(1);

        UpdatePasswordRequestDTO pDTO = UpdatePasswordRequestDTO.builder()
                .currentPassword("password")
                .newPassword("newPassword")
                .newPasswordConfirm("failConfirm")
                .build();

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userUpdateService.updatePassword(cmd.getUserId(), pDTO));
        assertThat(ex.getErrorCode()).isEqualTo(PASSWORD_MISMATCH);
    }

    @Test
    @Transactional
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호와 새 비밀번호 동일")
    void updatePassword_fail_samePassword() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email("upd04@algotalk.com")
                .loginId("upd04")
                .password(passwordEncoder.encode("samePassword"))
                .role("USER")
                .build();

        // USERS INSERT (userId 채번)
        userRegMapper.insertUser(cmd);
        assertThat(cmd.getUserId()).isNotNull();

        // USER_CREDENTIAL INSERT
        int credResult = userRegMapper.insertUserCredential(cmd);
        assertThat(credResult).isEqualTo(1);

        // USER_ROLES INSERT
        int roleResult = userRegMapper.insertUserRoles(cmd);
        assertThat(roleResult).isEqualTo(1);

        UpdatePasswordRequestDTO pDTO = UpdatePasswordRequestDTO.builder()
                .currentPassword("samePassword")
                .newPassword("samePassword")
                .newPasswordConfirm("samePassword")
                .build();

        // when, then
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userUpdateService.updatePassword(cmd.getUserId(), pDTO));
        assertThat(ex.getErrorCode()).isEqualTo(NOW_PASSWORD_SAME);
    }
}