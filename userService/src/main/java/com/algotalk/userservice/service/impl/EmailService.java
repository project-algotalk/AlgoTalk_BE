package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService implements IEmailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate stringRedisTemplate;

    // Redis Key Prefix
    private static final String AUTH_CODE_KEY = "email:auth:"; // 이메일 인증번호 저장 키 패턴
    private static final String VERIFIED_KEY = "email:verified:"; // 이메일 인증 완료 플래그 저장 키 패턴

    // TTL 설정
    @Value("${userservice.email.auth-code-ttl-minutes:3}")
    private long authCodeTtlMinutes; // 인증번호 TTL (기본 3분)

    @Value("${userservice.email.verified-ttl-minutes:30}")
    private long verifiedTtlMinutes; // 인증 완료 플래그 TTL (기본 30분)

    @Override
    public void sendEmailVerificationCode(String email) throws Exception {
        log.info("{}.sendEmailVerificationCode Start!", this.getClass().getName());

        log.info("email: {}", email);

        try {
            // 1. 6자리 인증번호 생성
            String code = generateVerificationCode();
            log.info("생성한 인증번호: {}", code);

            // 2. Redis에 인증번호 저장 (TTL 3분)
            String authCodeKey = AUTH_CODE_KEY + email;
            stringRedisTemplate.opsForValue().set(authCodeKey, code, authCodeTtlMinutes, TimeUnit.MINUTES);
            log.info("Redis에 인증번호 저장: key={}, value={}, ttl={}분", authCodeKey, code, authCodeTtlMinutes);

            // 3. 이메일 발송
            sendMail(email, code);
        } catch (BusinessException e) {
            log.error("{}.sendEmailVerificationCode BusinessException:, message={}", this.getClass().getName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("이메일 인증번호 발송 중 예상치 못한 오류 발생: {}", e.getMessage());
            throw new BusinessException(UserErrorCode.EMAIL_SEND_FAIL);
        } finally {
            log.info("{}.sendEmailVerificationCode End!", this.getClass().getName());
        }
    }

    @Override
    public void verifyEmailCode(String email, String code) throws Exception {
        log.info("{}.sendEmailVerificationCode Start!", this.getClass().getName());
        log.info("email: {}, code: {}", email, code);

        try {
            // 1. Redis에서 인증번호 조회
            String savedCode = stringRedisTemplate.opsForValue().get(AUTH_CODE_KEY + email);
            log.info("Redis에서 조회한 인증번호: {}", savedCode);

            // 2. 만료여부 확인
            if(savedCode == null) {
                log.info("인증번호가 만료되었거나 존재하지 않습니다.");
                throw new BusinessException(UserErrorCode.EMAIL_CODE_EXPIRED);
            }

            // 3. 입력값과 비교
            if(!savedCode.equals(code)) {
                log.info("인증번호가 일치하지 않습니다.");
                throw new BusinessException(UserErrorCode.EMAIL_CODE_MISMATCH);
            }

            // 4. 인증완료 플래그 저장 (TTL 30분)
            String verifiedKey = VERIFIED_KEY + email;
            stringRedisTemplate.opsForValue().set(verifiedKey, "true", verifiedTtlMinutes, TimeUnit.MINUTES);
            log.info("Redis에 인증완료 플래그 저장: key={}, value=true, ttl={}분", verifiedKey, verifiedTtlMinutes);

            // 5. Redis에서 인증번호 삭제
            stringRedisTemplate.delete(AUTH_CODE_KEY + email);
            log.info("Redis에서 인증번호 삭제: key={}", AUTH_CODE_KEY + email);
        } catch (BusinessException e) {
            log.error("{}.sendEmailVerificationCode BusinessException:, message={}", this.getClass().getName(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("이메일 인증 확인 중 예상치 못한 오류 발생: {}", e.getMessage());
            throw new BusinessException(UserErrorCode.EMAIL_SEND_FAIL);
        } finally {
            log.info("{}.sendEmailVerificationCode End!", this.getClass().getName());

        }
    }

    @Override
    public boolean isEmailVerified(String email) throws Exception {
        log.info("{}.isEmailVerified Start!", this.getClass().getName());
        log.info("email: {}", email);

        String verified = stringRedisTemplate.opsForValue().get(VERIFIED_KEY + email);
        log.info("Redis에서 조회한 인증완료 플래그: {}", verified);

        log.info("{}.isEmailVerified End!", this.getClass().getName());
        return "true".equals(verified);
    }

    private String generateVerificationCode() {
        // 6자리 인증번호 생성 로직
        int code = (int)(Math.random() * 900000) + 100000; // 100000 ~ 999999 사이의 랜덤 숫자
        return String.valueOf(code);
    }

    private void sendMail(String to, String code) throws Exception {
        // JavaMailSender를 사용하여 이메일 발송 로직 구현
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("[AlgoTalk] 이메일 인증번호 안내");
        message.setText(
                "안녕하세요. AlgoTalk입니다.\n\n" +
                        "이메일 인증번호: " + code + "\n\n" +
                        "인증번호는 3분간 유효합니다.\n"
        );
        mailSender.send(message);
        log.info("이메일 발송 완료 - to: {}", to);
    }
}
