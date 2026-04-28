package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.EmailSendRequestDTO;
import com.algotalk.userservice.dto.request.FindLoginIdRequestDTO;
import com.algotalk.userservice.dto.request.FindPasswordRequestDTO;
import com.algotalk.userservice.dto.request.ResetPasswordRequestDTO;
import com.algotalk.userservice.dto.response.UserInfoResponseDTO;
import com.algotalk.userservice.repository.IUserFindMapper;
import com.algotalk.userservice.service.IEmailService;
import com.algotalk.userservice.service.IFindAccountService;
import com.algotalk.userservice.util.CmmUtil;
import com.algotalk.userservice.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.algotalk.userservice.exception.UserErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindAccountService implements IFindAccountService {

    private final IUserFindMapper userFindMapper;
    private final IEmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String FIND_PASSWORD_KEY = "find:password:";

    @Override
    public void sendFindLoginIdEmail(FindLoginIdRequestDTO pDTO) throws Exception {
        log.info("{}.sendFindLoginIdEmail Start!", this.getClass().getName());

        // 1. 사용자 존재 여부 확인 (이름 + 이메일)
        UserInfoCommand pCommand = UserInfoCommand.builder()
                .name(CmmUtil.nvl(pDTO.name()))
                .email(EncryptUtil.encAES128CBC(CmmUtil.nvl(pDTO.email())))
                .build();

        UserInfoCommand rDTO = userFindMapper.findLoginIdByNameAndEmail(pCommand);

        if (rDTO == null) {
            log.info("사용자 정보가 존재하지 않습니다. name={}, email={}", pDTO.name(), pDTO.email());
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 2. 이메일 인증번호 발송
        emailService.sendEmailVerificationCode(EmailSendRequestDTO.builder()
                .email(pDTO.email())
                .build());

        log.info("{}.sendFindLoginIdEmail End!", this.getClass().getName());
    }

    @Override
    public UserInfoResponseDTO findLoginId(FindLoginIdRequestDTO pDTO) throws Exception {
        log.info("{}.findLoginId Start!", this.getClass().getName());

        // 1. 이메일 인증 완료 여부 확인
        if (!emailService.isEmailVerified(pDTO.email())) {
            log.info("이메일 인증이 완료되지 않았습니다. email={}", pDTO.email());
            throw new BusinessException(EMAIL_NOT_VERIFIED);
        }

        // 2. 사용자 정보 조회 (이름 + 이메일)
        UserInfoCommand pCommand = UserInfoCommand.builder()
                .name(CmmUtil.nvl(pDTO.name()))
                .email(EncryptUtil.encAES128CBC(CmmUtil.nvl(pDTO.email())))
                .build();

        UserInfoCommand rDTO = userFindMapper.findLoginIdByNameAndEmail(pCommand);

        if (rDTO == null) {
            log.info("사용자 정보가 존재하지 않습니다. name={}, email={}", pDTO.name(), pDTO.email());
            throw new BusinessException(USER_NOT_FOUND);
        }

        log.info("{}.findLoginId End!", this.getClass().getName());
        return UserInfoResponseDTO.builder()
                .loginId(rDTO.getLoginId())
                .build();
    }

    @Override
    public void sendFindPasswordEmail(FindPasswordRequestDTO pDTO) throws Exception {
        log.info("{}.sendFindPasswordEmail Start!", this.getClass().getName());

        // 1. 사용자 존재 여부 확인 (아이디 + 이름 + 이메일)
        UserInfoCommand pCommand = UserInfoCommand.builder()
                .loginId(CmmUtil.nvl(pDTO.loginId()))
                .name(CmmUtil.nvl(pDTO.name()))
                .email(EncryptUtil.encAES128CBC(CmmUtil.nvl(pDTO.email())))
                .build();

        UserInfoCommand rDTO = userFindMapper.findUserByLoginIdAndNameAndEmail(pCommand);

        if (rDTO == null) {
            log.info("사용자 정보가 존재하지 않습니다. loginId={}, name={}, email={}", pDTO.loginId(), pDTO.name(), pDTO.email());
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 2. Redis에 userId 임시 저장 (TTL 10분)
        stringRedisTemplate.opsForValue().set(
                FIND_PASSWORD_KEY + pDTO.email(),
                String.valueOf(rDTO.getUserId()),
                10, TimeUnit.MINUTES
        );

        // 2. 이메일 인증번호 발송
        emailService.sendEmailVerificationCode(EmailSendRequestDTO.builder()
                .email(pDTO.email())
                .build());

        log.info("{}.sendFindPasswordEmail End!", this.getClass().getName());
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequestDTO pDTO) throws Exception {
        log.info("{}.resetPassword Start!", this.getClass().getName());

        // 1. 이메일 인증 완료 여부 확인
        if (!emailService.isEmailVerified(pDTO.email())) {
            log.info("이메일 인증이 완료되지 않았습니다. email={}", pDTO.email());
            throw new BusinessException(EMAIL_NOT_VERIFIED);
        }

        // 2. 비밀번호 일치 여부 확인
        if (!pDTO.isPasswordConfirmed()) {
            log.warn("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
            throw new BusinessException(PASSWORD_MISMATCH);
        }

        // 3. Redis에서 userId 조회
        String userId = stringRedisTemplate.opsForValue().get(FIND_PASSWORD_KEY + pDTO.email());

        if (userId == null) {
            log.info("비밀번호 재설정 요청이 만료되었거나 존재하지 않습니다. email={}", pDTO.email());
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 4. 비밀번호 변경
        UserInfoCommand updateCommand = UserInfoCommand.builder()
                .userId(Long.parseLong(userId))
                .password(passwordEncoder.encode(CmmUtil.nvl(pDTO.newPassword())))
                .build();

        userFindMapper.updatePassword(updateCommand);

        // 5. Redis 정리 (find:password 키 삭제)
        stringRedisTemplate.delete(FIND_PASSWORD_KEY + pDTO.email());

        log.info("{}.resetPassword End!", this.getClass().getName());
    }
}
