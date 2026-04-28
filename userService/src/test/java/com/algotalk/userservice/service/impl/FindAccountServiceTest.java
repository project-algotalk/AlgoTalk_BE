package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.FindLoginIdRequestDTO;
import com.algotalk.userservice.dto.request.FindPasswordRequestDTO;
import com.algotalk.userservice.dto.request.ResetPasswordRequestDTO;
import com.algotalk.userservice.dto.response.UserInfoResponseDTO;
import com.algotalk.userservice.repository.IUserLoginMapper;
import com.algotalk.userservice.repository.IUserRegMapper;
import com.algotalk.userservice.service.IFindAccountService;
import com.algotalk.userservice.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class FindAccountServiceTest {

    @Autowired
    IFindAccountService findAccountService;

    @Autowired
    IUserRegMapper userRegMapper;
    @Autowired
    IUserLoginMapper userLoginMapper;

    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @Transactional
    @DisplayName("아이디 찾기 이메일 발송 실패 - 사용자 없음")
    void sendFindLoginIdEmail_userNotFound() {
        // given
        FindLoginIdRequestDTO pDTO = FindLoginIdRequestDTO.builder()
                .name("존재하지않는사람")
                .email("notexist@algotalk.com")
                .build();

        // when, then
        assertThrows(BusinessException.class, () ->
                findAccountService.sendFindLoginIdEmail(pDTO)
        );
    }

    @Test
    @Transactional
    @DisplayName("아이디 찾기 성공")
    void findLoginId_success() throws Exception {
        // given
        UserInfoCommand user = UserInfoCommand.builder()
                .nickname("테스트닉네임")
                .name("홍길동")
                .email(EncryptUtil.encAES128CBC("find02@algotalk.com"))
                .loginId("findtest02")
                .password("$2a$10$hashedpassword")
                .build();
        userRegMapper.insertUser(user);
        userRegMapper.insertUserCredential(user);

        // 이메일 인증 완료 플래그 설정
        stringRedisTemplate.opsForValue().set("email:verified:find02@algotalk.com", "Y");

        FindLoginIdRequestDTO pDTO = FindLoginIdRequestDTO.builder()
                .name("홍길동")
                .email("find02@algotalk.com")
                .build();

        // when
        UserInfoResponseDTO rDTO = findAccountService.findLoginId(pDTO);
        log.info("아이디 찾기 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.loginId()).isEqualTo("findtest02");

        // cleanup
        stringRedisTemplate.delete("email:verified:find02@algotalk.com");
    }

    @Test
    @DisplayName("아이디 찾기 실패 - 이메일 인증 안 됨")
    void findLoginId_emailNotVerified() {
        // given
        FindLoginIdRequestDTO pDTO = FindLoginIdRequestDTO.builder()
                .name("홍길동")
                .email("find02@algotalk.com")
                .build();

        // when, then
        assertThrows(BusinessException.class, () ->
                findAccountService.findLoginId(pDTO)
        );
    }

    @Test
    @Transactional
    @DisplayName("비밀번호 찾기 이메일 발송 실패 - 사용자 없음")
    void sendFindPasswordEmail_userNotFound() {
        // given
        FindPasswordRequestDTO pDTO = FindPasswordRequestDTO.builder()
                .loginId("notexist")
                .name("존재하지않는사람")
                .email("notexist@algotalk.com")
                .build();

        // when, then
        assertThrows(BusinessException.class, () ->
                findAccountService.sendFindPasswordEmail(pDTO)
        );
    }

    @Test
    @Transactional
    @DisplayName("비밀번호 재설정 실패 - 이메일 인증 안 됨")
    void resetPassword_emailNotVerified() {
        // given
        ResetPasswordRequestDTO pDTO = ResetPasswordRequestDTO.builder()
                .email("find03@algotalk.com")
                .newPassword("newPass1!")
                .newPasswordConfirm("newPass1!")
                .build();

        // when, then
        assertThrows(BusinessException.class, () ->
                findAccountService.resetPassword(pDTO)
        );
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 비밀번호 불일치")
    void resetPassword_passwordMismatch() {
        // given
        stringRedisTemplate.opsForValue().set("email:verified:find04@algotalk.com", "Y");

        ResetPasswordRequestDTO pDTO = ResetPasswordRequestDTO.builder()
                .email("find04@algotalk.com")
                .newPassword("newPass1!")
                .newPasswordConfirm("differentPass1!")
                .build();

        // when, then
        assertThrows(BusinessException.class, () ->
                findAccountService.resetPassword(pDTO)
        );

        // cleanup
        stringRedisTemplate.delete("email:verified:find04@algotalk.com");
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 세션 만료 (Redis userId 없음)")
    void resetPassword_sessionExpired() {
        // given
        stringRedisTemplate.opsForValue().set("email:verified:find05@algotalk.com", "Y");

        ResetPasswordRequestDTO pDTO = ResetPasswordRequestDTO.builder()
                .email("find05@algotalk.com")
                .newPassword("newPass1!")
                .newPasswordConfirm("newPass1!")
                .build();

        // when, then
        assertThrows(BusinessException.class, () ->
                findAccountService.resetPassword(pDTO)
        );

        // cleanup
        stringRedisTemplate.delete("email:verified:find05@algotalk.com");
    }

    @Test
    @Transactional
    @DisplayName("비밀번호 재설정 성공")
    void resetPassword_success() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트닉네임")
                .name("홍길동")
                .email(EncryptUtil.encAES128CBC("find06@algotalk.com"))
                .loginId("findtest06")
                .password("$2a$10$hashedpassword")
                .deletedYn("N")
                .build();
        // USERS INSERT (userId 채번)
        userRegMapper.insertUser(cmd);
        assertThat(cmd.getUserId()).isNotNull();

        // USER_CREDENTIAL INSERT
        int credResult = userRegMapper.insertUserCredential(cmd);
        assertThat(credResult).isEqualTo(1);

        // USER_ROLES INSERT
        int roleResult = userRegMapper.insertUserRoles(
                UserInfoCommand.builder()
                        .userId(cmd.getUserId())
                        .role("ROLE_USER") // 기본 역할로 "ROLE_USER" 저장
                        .build()
        );

        assertThat(roleResult).isEqualTo(1);

        // 이메일 인증 완료 플래그 설정
        stringRedisTemplate.opsForValue().set("email:verified:find06@algotalk.com", "Y");

        // Redis에 userId 저장 (sendFindPasswordEmail에서 저장되는 값 시뮬레이션)
        stringRedisTemplate.opsForValue().set(
                "find:password:find06@algotalk.com",
                String.valueOf(cmd.getUserId())
        );

        ResetPasswordRequestDTO pDTO = ResetPasswordRequestDTO.builder()
                .email("find06@algotalk.com")
                .newPassword("newPass1!")
                .newPasswordConfirm("newPass1!")
                .build();

        // when, then
        findAccountService.resetPassword(pDTO);

        UserInfoCommand rCommand = userLoginMapper.getUserAuthInfo(cmd);

        assertThat(passwordEncoder.matches(pDTO.newPassword(), rCommand.getPassword())).isTrue();

        // cleanup
        stringRedisTemplate.delete("email:verified:find06@algotalk.com");
    }
}