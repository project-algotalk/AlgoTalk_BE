package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.*;
import com.algotalk.userservice.dto.response.SignUpResponseDTO;
import com.algotalk.userservice.repository.IUserRegMapper;
import com.algotalk.userservice.service.IUserRegService;
import com.algotalk.userservice.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.parameters.P;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static java.time.LocalDate.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class UserRegServiceTest {

    @Autowired
    IUserRegService userRegService;

    @Autowired
    IUserRegMapper userRegMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("loginId 중복 확인 - 존재하지 않으면 false 반환")
    void isLoginIdDuplicated_notExists() throws Exception {
        // given
        LoginIdCheckRequestDTO pDTO = LoginIdCheckRequestDTO.builder()
                .loginId("not_exist_id")
                .build();

        // when
       userRegService.validateLoginIdUnique(pDTO);
//        log.info("loginId 중복 여부: {}", result);

        // then
//        assertThat(result).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("loginId 중복 확인 - 존재하면 true 반환")
    void isLoginIdDuplicated_exists() throws Exception {
        // given
        UserInfoCommand oldCmd = UserInfoCommand.builder()
                .nickname("플로우테스트")
                .name("테스트")
                .email("test@algotalk.com")
                .loginId("existing_id")
                .password("$2a$10$hashedpassword")
                .role("USER")
                .build();

        userRegMapper.insertUser(oldCmd);
        userRegMapper.insertUserCredential(oldCmd);
        assertThat(oldCmd.getUserId()).isNotNull();

        LoginIdCheckRequestDTO pDTO = LoginIdCheckRequestDTO.builder()
                .loginId("existing_id") // 실제 DB에 존재하는 loginId로 변경
                .build();

        // when, then
        assertThrows(BusinessException.class, () -> {
            userRegService.validateLoginIdUnique(pDTO);
        });
    }

    @Test
    @Transactional
    @DisplayName("nickname 중복 확인 - 존재하면 true 반환")
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

        NicknameCheckRequestDTO pDTO = NicknameCheckRequestDTO.builder()
                .nickname("중복 닉네임") // 실제 DB에 존재하는 nickname
                .build();

        // when, then
        assertThrows(BusinessException.class, () -> {
            userRegService.validateNicknameUnique(pDTO);
        });
    }

    @Test
    @DisplayName("email 중복 확인 - 존재하지 않으면 false 반환")
    void isEmailDuplicated_notExists() throws Exception {
        // given
        EmailCheckRequestDTO pDTO = EmailCheckRequestDTO.builder()
                .email("not_exist@algotalk.com")
                .build();

        // when
        userRegService.validateEmailUnique(pDTO);
//        log.info("email 중복 여부: {}", result);

        // then
//        assertThat(result).isFalse();
    }

    @Test
    @Transactional
    @DisplayName("email 중복 확인 - 존재하면 true 반환")
    void isEmailDuplicated_exists() throws Exception {
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

        EmailCheckRequestDTO pDTO = EmailCheckRequestDTO.builder()
                .email("test@algotalk.com") // 실제 DB에 존재하는 email
                .build();

        // when, then
        assertThrows(BusinessException.class, () -> {
            userRegService.validateEmailUnique(pDTO);
        });
    }

    @Test
    @Transactional
    @DisplayName("회원 가입 성공 - 기본 정보만")
    void insertUser_baseOnly() throws Exception {
        // given
        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .loginId("test")
                .password("test1234")
                .passwordConfirm("test1234")
                .email("test@algotalk.com")
                .name("홍길동")
                .nickname("둘리")
                .build();

        stringRedisTemplate.opsForValue()
                .set("email:verified:" + pDTO.email(), "Y");

        // when
        SignUpResponseDTO rDTO = userRegService.insertUser(pDTO);
        log.info("회원 가입 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.userId()).isNotNull();
        assertThat(rDTO.nickname()).isEqualTo(pDTO.nickname());
        assertThat(rDTO.email()).isEqualTo(pDTO.email());

        stringRedisTemplate.delete("email:verified:" + pDTO.email());
    }

    @Test
    @Transactional
    @DisplayName("회원 가입 성공 - 닉네임 미입력 시 이름으로 대체")
    void insertUser_nicknameResolvedFromName() throws Exception {
        // given
        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .loginId("test")
                .password("test1234")
                .passwordConfirm("test1234")
                .email("test@algotalk.com")
                .name("홍길동")
                .build();

        stringRedisTemplate.opsForValue()
                .set("email:verified:" + pDTO.email(), "Y");

        // when
        SignUpResponseDTO rDTO = userRegService.insertUser(pDTO);
        log.info("회원 가입 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.userId()).isNotNull();
        assertThat(rDTO.nickname()).isEqualTo(pDTO.name());
        assertThat(rDTO.email()).isEqualTo(pDTO.email());

        stringRedisTemplate.delete("email:verified:" + pDTO.email());
    }

    @Test
    @Transactional
    @DisplayName("회원 가입 성공 - 목표직무 한개 포함")
    void insertUser_OneTargetJob() throws Exception {
        // given
        List<TargetJobRequestDTO> targetJobs = new ArrayList<>();
        targetJobs.add(
                TargetJobRequestDTO.builder()
                        .categoryId(101L)
                        .categoryName("백엔드 개발자")
                        .startDate(of(2026, 3, 1))
                        .build()
        );

        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .loginId("test")
                .password("test1234")
                .passwordConfirm("test1234")
                .email("test@algotalk.com")
                .name("홍길동")
                .targetJobs(targetJobs)
                .build();

        stringRedisTemplate.opsForValue()
                .set("email:verified:" + pDTO.email(), "Y");

        // when
        SignUpResponseDTO rDTO = userRegService.insertUser(pDTO);
        log.info("회원 가입 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.userId()).isNotNull();
        assertThat(rDTO.nickname()).isEqualTo(pDTO.name());
        assertThat(rDTO.email()).isEqualTo(pDTO.email());
        assertThat(rDTO.targetJobs()).isNotNull();
        assertThat(rDTO.targetJobs()).hasSize(1);

        stringRedisTemplate.delete("email:verified:" + pDTO.email());
    }

    @Test
    @Transactional
    @DisplayName("회원 가입 성공 - 목표직무 세개 포함")
    void insertUser_ThreeTargetJob() throws Exception {
        // given
        List<TargetJobRequestDTO> targetJobs = new ArrayList<>();
        targetJobs.add(
                TargetJobRequestDTO.builder()
                        .categoryId(101L)
                        .categoryName("백엔드 개발자")
                        .startDate(of(2026, 3, 1))
                        .build()
        );

        targetJobs.add(
                TargetJobRequestDTO.builder()
                        .categoryId(102L)
                        .categoryName("풀스택 개발자")
                        .startDate(of(2026, 3, 1))
                        .build()
        );

        targetJobs.add(
                TargetJobRequestDTO.builder()
                        .categoryId(121L)
                        .categoryName("DevOps/SRE 엔지니어")
                        .startDate(of(2026, 3, 1))
                        .build()
        );

        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .loginId("test")
                .password("test1234")
                .passwordConfirm("test1234")
                .email("test@algotalk.com")
                .name("홍길동")
                .targetJobs(targetJobs)
                .build();

        stringRedisTemplate.opsForValue()
                .set("email:verified:" + pDTO.email(), "Y");

        // when
        SignUpResponseDTO rDTO = userRegService.insertUser(pDTO);
        log.info("회원 가입 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.userId()).isNotNull();
        assertThat(rDTO.nickname()).isEqualTo(pDTO.name());
        assertThat(rDTO.email()).isEqualTo(pDTO.email());
        assertThat(rDTO.targetJobs()).isNotNull();
        assertThat(rDTO.targetJobs()).hasSize(3);

        stringRedisTemplate.delete("email:verified:" + pDTO.email());
    }

    @Test
    @Transactional
    @DisplayName("회원 가입 성공 - 재직이력 두개 포함")
    void insertUser_TwoEmployment() throws Exception {
        // given
        List<EmploymentRequestDTO> employments = new ArrayList<>();
        employments.add(
                EmploymentRequestDTO.builder()
                        .categoryId(111L)
                        .categoryName("데이터 사이언티스트")
                        .companyName("알고톡")
                        .startDate(of(2022, 11, 1))
                        .endDate(of(2023, 12, 1))
                        .build()
        );

        employments.add(
                EmploymentRequestDTO.builder()
                        .categoryId(101L)
                        .categoryName("백엔드 개발자")
                        .companyName("비바리퍼블리카")
                        .startDate(of(2024, 1, 1))
                        .build()
        );


        SignUpRequestDTO pDTO = SignUpRequestDTO.builder()
                .loginId("test")
                .password("test1234")
                .passwordConfirm("test1234")
                .email("test@algotalk.com")
                .name("홍길동")
                .employments(employments)
                .build();

        stringRedisTemplate.opsForValue()
                .set("email:verified:" + pDTO.email(), "Y");

        // when
        SignUpResponseDTO rDTO = userRegService.insertUser(pDTO);
        log.info("회원 가입 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.userId()).isNotNull();
        assertThat(rDTO.nickname()).isEqualTo(pDTO.name());
        assertThat(rDTO.email()).isEqualTo(pDTO.email());

        stringRedisTemplate.delete("email:verified:" + pDTO.email());
    }


}