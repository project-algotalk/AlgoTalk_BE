package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.UpdateAddrRequestDTO;
import com.algotalk.userservice.dto.request.UpdateNameRequestDTO;
import com.algotalk.userservice.dto.request.UpdateNicknameRequestDTO;
import com.algotalk.userservice.dto.request.UpdatePasswordRequestDTO;
import com.algotalk.userservice.repository.IUserRegMapper;
import com.algotalk.userservice.service.IUserUpdateService;
import com.algotalk.userservice.util.EncryptUtil;
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
                .email(EncryptUtil.encAES128CBC("upd01@algotalk.com"))
                .loginId("upd01")
                .password(passwordEncoder.encode("password"))
                .passwordSetYn("Y")
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
                .email(EncryptUtil.encAES128CBC("upd02@algotalk.com"))
                .loginId("upd02")
                .password(passwordEncoder.encode("password"))
                .passwordSetYn("Y")
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
                .email(EncryptUtil.encAES128CBC("upd03@algotalk.com"))
                .loginId("upd03")
                .password(passwordEncoder.encode("password"))
                .passwordSetYn("Y")
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
                .email(EncryptUtil.encAES128CBC("upd04@algotalk.com"))
                .loginId("upd04")
                .password(passwordEncoder.encode("samePassword"))
                .passwordSetYn("Y")
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

    @Test
    @Transactional
    @DisplayName("닉네임 변경 - 성공")
    public void updateNickname_success() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("upd05@algotalk.com"))
                .loginId("upd05")
                .password(passwordEncoder.encode("password"))
                .passwordSetYn("Y")
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

        UpdateNicknameRequestDTO pDTO = UpdateNicknameRequestDTO.builder()
                .nickname("닉네임변경")
                .build();

        // when
        int res = userUpdateService.updateNickname(cmd.getUserId(), pDTO);

        // then
        assertThat(res).isEqualTo(1);
    }
    
    @Test
    @Transactional
    @DisplayName("닉네임 변경 - 닉네임 중복")
    public void updateNickname_fail_duplicate() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("upd06@algotalk.com"))
                .loginId("upd06")
                .password(passwordEncoder.encode("password"))
                .passwordSetYn("Y")
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

        // when
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userUpdateService.updateNickname(cmd.getUserId(), UpdateNicknameRequestDTO.builder()
                        .nickname("테스트유저") // 기존 닉네임과 동일한 값으로 변경 시도
                        .build()));
    
        // then
        assertThat(ex.getErrorCode()).isEqualTo(DUPLICATE_NICKNAME);
    }

    @Test
    @Transactional
    @DisplayName("이름 변경 - 성공")
    public void updateName_success() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("upd07@algotalk.com"))
                .loginId("upd07")
                .password(passwordEncoder.encode("password"))
                .passwordSetYn("Y")
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

        UpdateNameRequestDTO pDTO = UpdateNameRequestDTO.builder()
                .name("이름변경")
                .build();

        // when
        int res = userUpdateService.updateName(cmd.getUserId(), pDTO);

        // then
        assertThat(res).isEqualTo(1);
    }

    @Test
    @Transactional
    @DisplayName("주소 변경 - 성공")
    public void updateAddr_success() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("upd07@algotalk.com"))
                .loginId("upd07")
                .password(passwordEncoder.encode("password"))
                .passwordSetYn("Y")
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

        UpdateAddrRequestDTO pDTO = UpdateAddrRequestDTO.builder()
                .addr1("주소1 변경")
                .addr2("주소2 변경")
                .build();

        // when
        int res = userUpdateService.updateAddr(cmd.getUserId(), pDTO);

        // then
        assertThat(res).isEqualTo(1);
    }
}