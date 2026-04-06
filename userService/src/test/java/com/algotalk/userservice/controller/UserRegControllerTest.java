package com.algotalk.userservice.controller;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.LoginIdCheckRequestDTO;
import com.algotalk.userservice.dto.request.SignUpRequestDTO;
import com.algotalk.userservice.dto.request.TargetJobRequestDTO;
import com.algotalk.userservice.repository.IUserRegMapper;
import com.algotalk.userservice.service.IEmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UserRegControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    private IUserRegMapper userRegMapper;

    @MockBean
    private IEmailService emailService;

    @Test
    @Transactional
    @DisplayName("loginId 중복 확인 - 중복 되지 않은 경우")
    void isLoginIdDuplicated_notExists() throws Exception {
        // given
        LoginIdCheckRequestDTO pDTO = LoginIdCheckRequestDTO.builder()
                .loginId("notExistId")
                .build();

        // when & then
        mockMvc.perform(post("/user/v1/reg/check/loginId")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data").value(false))
        ;
    }

    @Test
    @Transactional
    @DisplayName("loginId 중복 확인 - 중복된 경우")
    void isLoginIdDuplicated_exists() throws Exception {
        // given
        UserInfoCommand oldCmd = UserInfoCommand.builder()
                .nickname("플로우테스트")
                .name("테스트")
                .email("test@algotalk.com")
                .loginId("existId")
                .password("$2a$10$hashedpassword")
                .role("USER")
                .build();

        userRegMapper.insertUser(oldCmd);
        userRegMapper.insertUserCredential(oldCmd);
        assertThat(oldCmd.getUserId()).isNotNull();

        LoginIdCheckRequestDTO pDTO = LoginIdCheckRequestDTO.builder()
                .loginId("existId") // 실제 DB에 존재하는 loginId로 변경
                .build();

        // 2. 중복되는 로그인 아이디로 테스트
        mockMvc.perform(post("/user/v1/reg/check/loginId")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
        ;
    }

    @Test
    @DisplayName("loginId 중복 확인 - loginId 누락")
    void checkLoginId_missingLoginId()  throws Exception {
        // given
        LoginIdCheckRequestDTO pDTO = LoginIdCheckRequestDTO.builder()
                .loginId("") // 빈 문자열로 설정
                .build();

        // when & then
        mockMvc.perform(post("/user/v1/reg/check/loginId")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
        ;
    }

    @Test
    @Transactional
    @DisplayName("nickname 중복 확인 - 중복되지 않는 경우")
    void isNicknameDuplicated_notExists() throws Exception {
        // given
        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .nickname("홍길동")
                .build();

        // when & then
        mockMvc.perform(post("/user/v1/reg/check/nickname")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data").value(false))
        ;
    }

    @Test
    @Transactional
    @DisplayName("nickname 중복 확인 - 중복되는 경우")
    void isNicknameDuplicated_exists() throws Exception {
        // given
        UserInfoCommand oldCmd = UserInfoCommand.builder()
                .nickname("중복 닉네임")
                .name("테스트")
                .email("test@algotalk.com")
                .loginId("test")
                .password("$2a$10$hashedpassword")
                .role("USER")
                .build();

        userRegMapper.insertUser(oldCmd);
        assertThat(oldCmd.getUserId()).isNotNull();

        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .nickname("중복 닉네임") // 실제 DB에 존재하는 nickname
                .build();

        // when & then
        mockMvc.perform(post("/user/v1/reg/check/nickname")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError())
        ;
    }

    @Test
    @Transactional
    @DisplayName("email 중복 확인 - 존재하지 않는 경우")
    void isEmailDuplicated_notExists() throws Exception {
        // given
        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .email("not_exist@algotalk.com")
                .build();

        // when & then
        mockMvc.perform(post("/user/v1/reg/check/email")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.data").value(false))
        ;
    }


    @Test
    @Transactional
    @DisplayName("회원가입 성공 - 기본정보만")
    void signUp_basicOnly() throws Exception {
        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .loginId("ctrltest01")
                .password("Test1234!")
                .passwordConfirm("Test1234!")
                .email("ctrl01@algotalk.com")
                .name("홍길동")
                .nickname("길동이")
                .build();

        given(emailService.isEmailVerified(any()))
                .willReturn(true);

        mockMvc.perform(post("/user/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andExpect(jsonPath("$.data.nickname").value("길동이"));
    }

    @Test
    @Transactional
    @DisplayName("회원가입 성공 - 목표직무 포함")
    void signUp_withTargetJobs() throws Exception {
        // given
        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .loginId("ctrltest02")
                .password("test1234!")
                .passwordConfirm("test1234!")
                .email("ctrl02@algotalk.com")
                .name("홍길동")
                .nickname("길동이2")
                .targetJobs(List.of(
                        TargetJobRequestDTO.builder()
                                .categoryId(101L)
                                .categoryName("백엔드 개발자")
                                .startDate(LocalDate.of(2026, 1, 1))
                                .build()
                ))
                .build();

        given(emailService.isEmailVerified(any()))
                .willReturn(true);

        // when & then
        mockMvc.perform(post("/user/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").isNotEmpty())
                .andExpect(jsonPath("$.data.targetJobs[0]").value("백엔드 개발자"));
    }

    @Test
    @Transactional
    @DisplayName("회원가입 실패 - 비밀번호 불일치")
    void signUp_passwordMismatch() throws Exception {
        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .loginId("ctrltest03")
                .password("Test1234!")
                .passwordConfirm("Wrong1234!")
                .email("ctrl03@algotalk.com")
                .name("홍길동")
                .build();

        mockMvc.perform(post("/user/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("회원가입 실패 - 필수 입력값 누락")
    void signUp_missingRequiredFields() throws Exception {
        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .password("Test1234!")
                .passwordConfirm("Test1234!")
                .email("")
                .build();

        mockMvc.perform(post("/user/v1/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pDTO)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }
}