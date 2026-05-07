package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.*;
import com.algotalk.userservice.dto.response.SignUpResponseDTO;
import com.algotalk.userservice.repository.IUserRegMapper;
import com.algotalk.userservice.service.IUserRegService;
import com.algotalk.userservice.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static java.time.LocalDate.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserRegServiceTest {

    @Autowired
    IUserRegService userRegService;

    @Autowired
    IUserRegMapper userRegMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("loginId 중복 확인 - 존재하지 않으면 false 반환")
    void isLoginIdDuplicated_notExists() throws Exception {
        // given
        CheckLoginIdRequestDTO pDTO = CheckLoginIdRequestDTO.builder()
                .loginId("not_exist_id")
                .build();

        // when, then
        assertDoesNotThrow(() -> userRegService.validateLoginIdUnique(pDTO));
    }

    @Test
    @Transactional
    @DisplayName("loginId 중복 확인 - 존재하면 true 반환")
    void isLoginIdDuplicated_exists() throws Exception {
        // given
        UserInfoCommand oldCmd = UserInfoCommand.builder()
                .nickname("플로우테스트")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("reg01@algotalk.com"))
                .loginId("reg01")
                .password("$2a$10$hashedpassword")
                .role("USER")
                .build();

        userRegMapper.insertUser(oldCmd);
        userRegMapper.insertUserCredential(oldCmd);
        assertThat(oldCmd.getUserId()).isNotNull();

        CheckLoginIdRequestDTO pDTO = CheckLoginIdRequestDTO.builder()
                .loginId("reg01") // 실제 DB에 존재하는 loginId로 변경
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
                .email(EncryptUtil.encAES128CBC("reg02@algotalk.com"))
                .loginId("reg02")
                .password("$2a$10$hashedpassword")
                .role("USER")
                .build();

        userRegMapper.insertUser(oldCmd);
        assertThat(oldCmd.getUserId()).isNotNull();

        CheckNicknameRequestDTO pDTO = CheckNicknameRequestDTO.builder()
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
        CheckEmailRequestDTO pDTO = CheckEmailRequestDTO.builder()
                .email("not_exist@algotalk.com")
                .build();

        // when, then
        assertDoesNotThrow(() -> userRegService.validateEmailUnique(pDTO));
    }

    @Test
    @Transactional
    @DisplayName("email 중복 확인 - 존재하면 true 반환")
    void isEmailDuplicated_exists() throws Exception {
        // given
        UserInfoCommand oldCmd = UserInfoCommand.builder()
                .nickname("중복 닉네임")
                .name("테스트")
                .email(EncryptUtil.encAES128CBC("reg03@algotalk.com"))
                .loginId("reg03")
                .password("$2a$10$hashedpassword")
                .role("USER")
                .build();

        userRegMapper.insertUser(oldCmd);
        assertThat(oldCmd.getUserId()).isNotNull();

        CheckEmailRequestDTO pDTO = CheckEmailRequestDTO.builder()
                .email("reg03@algotalk.com") // 실제 DB에 존재하는 email
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
                .loginId("reg04")
                .password("test1234")
                .passwordConfirm("test1234")
                .email(EncryptUtil.encAES128CBC("reg04@algotalk.com"))
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
                .loginId("reg05")
                .password("test1234")
                .passwordConfirm("test1234")
                .email(EncryptUtil.encAES128CBC("reg05@algotalk.com"))
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
                .loginId("reg06")
                .password("test1234")
                .passwordConfirm("test1234")
                .email(EncryptUtil.encAES128CBC("reg06@algotalk.com"))
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
                .loginId("reg07")
                .password("test1234")
                .passwordConfirm("test1234")
                .email(EncryptUtil.encAES128CBC("reg07@algotalk.com"))
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
                .loginId("reg08")
                .password("test1234")
                .passwordConfirm("test1234")
                .email(EncryptUtil.encAES128CBC("reg08@algotalk.com"))
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

    @Test
    @Transactional
    @DisplayName("소셜 회원가입 성공 - 기본 정보만")
    void insertSocialUser_baseOnly() throws Exception {
        // given
        String tempToken = "test-temp-token-" + UUID.randomUUID();
        String redisKey = "oauth2:temp:" + tempToken;

        Map<String, String> tempData = new HashMap<>();
        tempData.put("provider",   "GOOGLE");
        tempData.put("providerId", "test-provider-id-001");
        tempData.put("email",      "social01@gmail.com");
        tempData.put("name",       "홍길동");
        redisTemplate.opsForHash().putAll(redisKey, tempData);

        SocialSignUpRequestDTO pDTO = SocialSignUpRequestDTO.builder()
                .tempToken(tempToken)
                .build();

        // when
        SignUpResponseDTO rDTO = userRegService.insertSocialUser(pDTO);
        log.info("소셜 회원가입 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.userId()).isNotNull();
        assertThat(rDTO.nickname()).isNotNull();
        assertThat(rDTO.email()).isEqualTo("social01@gmail.com");
    }

    @Test
    @Transactional
    @DisplayName("소셜 회원가입 성공 - 목표직무 포함")
    void insertSocialUser_withTargetJob() throws Exception {
        // given
        String tempToken = "test-temp-token-" + UUID.randomUUID();
        String redisKey = "oauth2:temp:" + tempToken;

        Map<String, String> tempData = new HashMap<>();
        tempData.put("provider",   "GOOGLE");
        tempData.put("providerId", "test-provider-id-002");
        tempData.put("email",      "social02@gmail.com");
        tempData.put("name",       "김철수");
        redisTemplate.opsForHash().putAll(redisKey, tempData);

        List<TargetJobRequestDTO> targetJobs = new ArrayList<>();
        targetJobs.add(
                TargetJobRequestDTO.builder()
                        .categoryId(101L)
                        .categoryName("백엔드 개발자")
                        .startDate(of(2026, 3, 1))
                        .build()
        );

        SocialSignUpRequestDTO pDTO = SocialSignUpRequestDTO.builder()
                .tempToken(tempToken)
                .targetJobs(targetJobs)
                .build();

        // when
        SignUpResponseDTO rDTO = userRegService.insertSocialUser(pDTO);
        log.info("소셜 회원가입 결과: {}", rDTO);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.userId()).isNotNull();
        assertThat(rDTO.targetJobs()).hasSize(1);

        // cleanup
        redisTemplate.delete(redisKey);
    }

    @Test
    @DisplayName("소셜 회원가입 실패 - 임시 토큰 없음")
    void insertSocialUser_tempTokenNotFound() {
        // given
        SocialSignUpRequestDTO pDTO = SocialSignUpRequestDTO.builder()
                .tempToken("not-exist-token")
                .build();

        // when, then
        assertThrows(BusinessException.class, () -> {
            userRegService.insertSocialUser(pDTO);
        });
    }

}