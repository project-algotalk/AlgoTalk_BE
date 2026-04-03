package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.request.EmailCheckRequestDTO;
import com.algotalk.userservice.dto.request.EmailSendRequestDTO;
import com.algotalk.userservice.dto.request.EmailVerifyRequestDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.service.IEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
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
    public void sendEmailVerificationCode(EmailSendRequestDTO pDTO) throws Exception {
        log.info("{}.sendEmailVerificationCode Start!", this.getClass().getName());
        log.info("EmailSendRequestDTO: {}", pDTO);
        String email = pDTO.email();

        // 1. 6자리 인증번호 생성
        String code = generateVerificationCode();

        // 2. 이메일 발송
        sendMail(email, code);

        // 3. Redis에 인증번호 저장 (TTL 3분)
        String authCodeKey = AUTH_CODE_KEY + email;
        stringRedisTemplate.opsForValue().set(authCodeKey, code, authCodeTtlMinutes, TimeUnit.MINUTES);
        log.info("Redis에 인증번호 저장: key={}, value={}, ttl={}분", authCodeKey, code, authCodeTtlMinutes);

        log.info("{}.sendEmailVerificationCode End!", this.getClass().getName());
    }

    @Override
    public void verifyEmailCode(EmailVerifyRequestDTO pDTO) throws Exception {
        log.info("{}.verifyEmailCode Start!", this.getClass().getName());
        String email = pDTO.email();
        String authNumber = pDTO.authNumber();

        // 1. Redis에서 인증번호 조회
        String savedCode = stringRedisTemplate.opsForValue().get(AUTH_CODE_KEY + email);
        log.info("Redis에서 조회한 인증번호: {}", savedCode);

        // 2. 만료여부 확인
        if(savedCode == null) {
            log.info("인증번호가 만료되었거나 존재하지 않습니다.");
            throw new BusinessException(UserErrorCode.EMAIL_CODE_EXPIRED);
        }

        // 3. 입력값과 비교
        if(!savedCode.equals(authNumber)) {
            log.info("인증번호가 일치하지 않습니다.");
            throw new BusinessException(UserErrorCode.EMAIL_CODE_MISMATCH);
        }

        // 4. 인증완료 플래그 저장 (TTL 30분)
        String verifiedKey = VERIFIED_KEY + email;
        stringRedisTemplate.opsForValue().set(verifiedKey, "Y", verifiedTtlMinutes, TimeUnit.MINUTES);
        log.info("Redis에 인증완료 플래그 저장: key={}, value=true, ttl={}분", verifiedKey, verifiedTtlMinutes);

        // 5. Redis에서 인증번호 삭제
        stringRedisTemplate.delete(AUTH_CODE_KEY + email);
        log.info("Redis에서 인증번호 삭제: key={}", AUTH_CODE_KEY + email);

        log.info("{}.verifyEmailCode End!", this.getClass().getName());
    }

    @Override
    public boolean isEmailVerified(String email) throws Exception {
        log.info("{}.isEmailVerified Start!", this.getClass().getName());
        log.info("email: {}", email);

        String verified = stringRedisTemplate.opsForValue().get(VERIFIED_KEY + email);
        log.info("Redis에서 조회한 인증완료 플래그: {}", verified);

        log.info("{}.isEmailVerified End!", this.getClass().getName());
        return "Y".equals(verified);
    }

    private String generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        // 6자리 인증번호 생성 로직
        // 0 ~ 999999 사이의 난수 생성
        int authCode = random.nextInt(1000000);

        // %06d: 6자리 숫자로 포맷팅, 빈 공간은 0으로 채움
        return String.format("%06d", authCode);
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
