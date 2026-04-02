package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.service.IEmailService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Slf4j
@SpringBootTest
@ActiveProfiles("local")
class EmailServiceTest {

    @Autowired
    IEmailService emailService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private static final String TEST_EMAIL = "gnstm831@gmail.com";

    @Test
    @DisplayName("이메일 인증번호 발송 - Redis 저장 확인")
    void sendEmailVerificationCode_redisStored() throws Exception {
        // when
        emailService.sendEmailVerificationCode(TEST_EMAIL);

        // then: Redis에 저장됐는지 확인
        String savedCode = stringRedisTemplate.opsForValue()
                .get("email:auth:" + TEST_EMAIL);

        log.info("Redis 저장된 인증번호: {}", savedCode);

        assertThat(savedCode).isNotNull();
        assertThat(savedCode).hasSize(6);
        assertThat(savedCode).matches("\\d{6}");  // 6자리 숫자
    }

    @Test
    @DisplayName("이메일 인증번호 확인 - 올바른 코드 입력 시 인증 완료")
    void verifyEmailCode_success() throws Exception {
        // given: 인증번호 발송 후 Redis에서 직접 조회
        emailService.sendEmailVerificationCode(TEST_EMAIL);
        String code = stringRedisTemplate.opsForValue()
                .get("email:auth:" + TEST_EMAIL);

        log.info("테스트용 인증번호: {}", code);
        assertThat(code).isNotNull();

        // when: 올바른 코드 입력
        emailService.verifyEmailCode(TEST_EMAIL, code);

        // then: 인증 완료 플래그 확인
        String verified = stringRedisTemplate.opsForValue()
                .get("email:verified:" + TEST_EMAIL);
        assertThat(verified).isEqualTo("true");

        // then: 인증번호 삭제 확인
        String deletedCode = stringRedisTemplate.opsForValue()
                .get("email:auth:" + TEST_EMAIL);
        assertThat(deletedCode).isNull();

        // 테스트 후 정리
        stringRedisTemplate.delete("email:verified:" + TEST_EMAIL);
    }

    @Test
    @DisplayName("이메일 인증번호 확인 - 틀린 코드 입력 시 예외 발생")
    void verifyEmailCode_wrongCode() throws Exception {
        // given
        emailService.sendEmailVerificationCode(TEST_EMAIL);

        // when & then
        assertThatThrownBy(() ->
                emailService.verifyEmailCode(TEST_EMAIL, "000000")  // 잘못된 코드
        ).isInstanceOf(BusinessException.class);

        // 테스트 후 정리
        stringRedisTemplate.delete("email:auth:" + TEST_EMAIL);
    }

    @Test
    @DisplayName("이메일 인증번호 확인 - 만료된 코드 입력 시 예외 발생")
    void verifyEmailCode_expired() {
        // given: Redis에 데이터 없음 (만료 시뮬레이션)
        stringRedisTemplate.delete("email:auth:" + TEST_EMAIL);

        // when & then
        assertThatThrownBy(() ->
                emailService.verifyEmailCode(TEST_EMAIL, "123456")
        ).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("이메일 인증 완료 여부 - 미인증 시 false")
    void isEmailVerified_notVerified() throws Exception {
        // given: verified 키 없음
        stringRedisTemplate.delete("email:verified:" + TEST_EMAIL);

        // when & then
        boolean result = emailService.isEmailVerified(TEST_EMAIL);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("이메일 인증 완료 여부 - 인증 완료 시 true")
    void isEmailVerified_verified() throws Exception {
        // given: verified 플래그 직접 세팅
        stringRedisTemplate.opsForValue()
                .set("email:verified:" + TEST_EMAIL, "true");

        // when
        boolean result = emailService.isEmailVerified(TEST_EMAIL);
        assertThat(result).isTrue();

        // 테스트 후 정리
        stringRedisTemplate.delete("email:verified:" + TEST_EMAIL);
    }
}