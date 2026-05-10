package com.algotalk.userservice.controller;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.UpdateAddrRequestDTO;
import com.algotalk.userservice.dto.request.UpdateNameRequestDTO;
import com.algotalk.userservice.dto.request.UpdateNicknameRequestDTO;
import com.algotalk.userservice.dto.request.UpdatePasswordRequestDTO;
import com.algotalk.userservice.repository.IUserRegMapper;
import com.algotalk.userservice.util.EncryptUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import static com.algotalk.userservice.exception.UserErrorCode.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IUserRegMapper userRegMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // JWT mock 생성 헬퍼 - userId만 subject로 넣으면 됨
    private MockHttpServletRequestBuilder withMockJwt(MockHttpServletRequestBuilder request, Long userId) {
        return request.with(
                SecurityMockMvcRequestPostProcessors.jwt()
                        .jwt(jwt -> jwt.subject(String.valueOf(userId)))
        );
    }

    @Test
    @Transactional
    @DisplayName("비밀번호 변경 성공")
    void updatePassword_success() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email("ctrl01@algotalk.com")
                .loginId("ctrl01")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdatePasswordRequestDTO pDTO = UpdatePasswordRequestDTO.builder()
                .currentPassword("CurrentPass1!")
                .newPassword("NewPass1!")
                .newPasswordConfirm("NewPass1!")
                .build();

        // when, then
        mockMvc.perform(
                withMockJwt(
                        post("/mypage/v1/update-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)),
                        cmd.getUserId()
                ))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("비밀번호 변경 실패 - JWT 없음")
    void updatePassword_fail_noJwt() throws Exception {
        // given
        UpdatePasswordRequestDTO pDTO = UpdatePasswordRequestDTO.builder()
                .currentPassword("CurrentPass1!")
                .newPassword("NewPass1!")
                .newPasswordConfirm("NewPass1!")
                .build();

        // when, then
        mockMvc.perform(
                post("/mypage/v1/update-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andExpect(status().isUnauthorized())
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 입력 안함")
    void updatePassword_fail_validation() throws Exception {
        // given - @NotBlank
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email("ctrl03@algotalk.com")
                .loginId("ctrl03")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdatePasswordRequestDTO pDTO = UpdatePasswordRequestDTO.builder()
                .currentPassword("")
                .newPassword("NewPass1!")
                .newPasswordConfirm("NewPass1!")
                .build();

        // when, then
        mockMvc.perform(
                withMockJwt(
                        post("/mypage/v1/update-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)),
                        cmd.getUserId()
                ))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("비밀번호 변경 실패 - 새로운 비밀번호와 확인 비밀번호 불일치")
    void updatePassword_fail_newPasswordMismatch() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email("ctrl03@algotalk.com")
                .loginId("ctrl03")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdatePasswordRequestDTO pDTO = UpdatePasswordRequestDTO.builder()
                .currentPassword("CurrentPass1!")
                .newPassword("NewPass1!")
                .newPasswordConfirm("NotMatch1!")
                .build();

        // when, then
        mockMvc.perform(
                withMockJwt(
                        post("/mypage/v1/update-password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(pDTO)),
                        cmd.getUserId()
                ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(PASSWORD_MISMATCH.getCode())) // 커스텀 에러 코드 확인!
                .andExpect(jsonPath("$.message").exists())     // 에러 메시지가 존재하는지 확인
                .andDo(print());
    }


    @Test
    @Transactional
    @DisplayName("비밀번호 변경 실패 - 현재 비밀번호 불일치")
    void updatePassword_fail_currentPasswordMismatch() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email("ctrl03@algotalk.com")
                .loginId("ctrl03")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdatePasswordRequestDTO pDTO = UpdatePasswordRequestDTO.builder()
                .currentPassword("NotMatch1!")
                .newPassword("NewPass1!")
                .newPasswordConfirm("NewPass1!")
                .build();

        // when, then
        mockMvc.perform(
                        withMockJwt(
                                post("/mypage/v1/update-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pDTO)),
                                cmd.getUserId()
                        ))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(CUR_PASSWORD_MISMATCH.getCode())) // 커스텀 에러 코드 확인!
                .andExpect(jsonPath("$.message").exists())     // 에러 메시지가 존재하는지 확인
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("닉네임 변경 성공")
    void updateNickname_success() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("nickname01@algotalk.com"))
                .loginId("nickname01")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdateNicknameRequestDTO pDTO = UpdateNicknameRequestDTO.builder()
                        .nickname("닉네임변경")
                                .build();

        // when, then
        mockMvc.perform(
                        withMockJwt(
                                post("/mypage/v1/update-nickname")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pDTO)),
                                cmd.getUserId()
                        ))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("닉네임 변경 실패 - Size 미만 입력")
    void updateNickname_fail_underMinSize() throws Exception {
        // given - @Size(min=2, max=20)
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("nickname02@algotalk.com"))
                .loginId("nickname02")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdateNicknameRequestDTO pDTO = UpdateNicknameRequestDTO.builder()
                .nickname("닉")
                .build();

        // when, then
        mockMvc.perform(
                        withMockJwt(
                                post("/mypage/v1/update-nickname")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pDTO)),
                                cmd.getUserId()
                        ))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("닉네임 변경 실패 - Size 초과 입력")
    void updateNickname_fail_overMaxSize() throws Exception {
        // given - @Size(min=2, max=20)
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("nickname03@algotalk.com"))
                .loginId("nickname03")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdateNicknameRequestDTO pDTO = UpdateNicknameRequestDTO.builder()
                .nickname("닉네임이10글자를넘어갑니다")
                .build();

        // when, then
        mockMvc.perform(
                        withMockJwt(
                                post("/mypage/v1/update-nickname")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pDTO)),
                                cmd.getUserId()
                        ))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("닉네임 변경 실패 - Size 초과 입력")
    void updateNickname_fail_specialChar() throws Exception {
        // given - @Pattern(regexp="^[a-zA-Z0-9가-힣]*$")
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("nickname04@algotalk.com"))
                .loginId("nickname04")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdateNicknameRequestDTO pDTO = UpdateNicknameRequestDTO.builder()
                .nickname("닉네임!!")
                .build();

        // when, then
        mockMvc.perform(
                        withMockJwt(
                                post("/mypage/v1/update-nickname")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pDTO)),
                                cmd.getUserId()
                        ))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("닉네임 변경 실패 - 중복된 닉네임 입력")
    void updateNickname_fail_duplicate() throws Exception {
        // given - @Pattern(regexp="^[a-zA-Z0-9가-힣]*$")
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("nickname05@algotalk.com"))
                .loginId("nickname05")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdateNicknameRequestDTO pDTO = UpdateNicknameRequestDTO.builder()
                .nickname("테스트유저")
                .build();

        // when, then
        mockMvc.perform(
                        withMockJwt(
                                post("/mypage/v1/update-nickname")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pDTO)),
                                cmd.getUserId()
                        ))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(DUPLICATE_NICKNAME.getCode())) // 커스텀 에러 코드 확인!
                .andExpect(jsonPath("$.message").exists())     // 에러 메시지가 존재하는지 확인
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("이름 변경 성공")
    void updateName_success() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("name01@algotalk.com"))
                .loginId("name01")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdateNameRequestDTO pDTO = UpdateNameRequestDTO.builder()
                .name("이름변경")
                .build();

        // when, then
        mockMvc.perform(
                        withMockJwt(
                                post("/mypage/v1/update-name")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pDTO)),
                                cmd.getUserId()
                        ))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("닉네임 변경 실패 - null 입력")
    void updateName_fail_validation() throws Exception {
        // given - @NotBlank
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("nickname04@algotalk.com"))
                .loginId("nickname04")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdateNameRequestDTO pDTO = UpdateNameRequestDTO.builder()
                .build();

        // when, then
        mockMvc.perform(
                        withMockJwt(
                                post("/mypage/v1/update-name")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pDTO)),
                                cmd.getUserId()
                        ))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("닉네임 변경 실패 - 공백 입력")
    void updateName_fail_validation2() throws Exception {
        // given - @NotBlank
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("nickname04@algotalk.com"))
                .loginId("nickname04")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdateNameRequestDTO pDTO = UpdateNameRequestDTO.builder()
                .name("   ")
                .build();

        // when, then
        mockMvc.perform(
                        withMockJwt(
                                post("/mypage/v1/update-name")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pDTO)),
                                cmd.getUserId()
                        ))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @Transactional
    @DisplayName("주소 변경 성공")
    void updateAddr_success() throws Exception {
        // given
        UserInfoCommand cmd = UserInfoCommand.builder()
                .nickname("테스트유저")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("addr01@algotalk.com"))
                .loginId("addr01")
                .password(passwordEncoder.encode("CurrentPass1!"))
                .passwordSetYn("Y")
                .role("USER")
                .build();

        userRegMapper.insertUser(cmd);
        userRegMapper.insertUserCredential(cmd);
        userRegMapper.insertUserRoles(cmd);

        UpdateAddrRequestDTO pDTO = UpdateAddrRequestDTO.builder()
                .addr1("새로운 주소1")
                .addr2("새로운 주소2")
                .build();

        // when, then
        mockMvc.perform(
                        withMockJwt(
                                post("/mypage/v1/update-addr")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(pDTO)),
                                cmd.getUserId()
                        ))
                .andExpect(status().isOk())
                .andDo(print());
    }
}