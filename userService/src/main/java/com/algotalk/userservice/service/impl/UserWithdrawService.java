package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.domain.enums.LoginType;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.WithdrawRequestDTO;
import com.algotalk.userservice.repository.IUserUpdateMapper;
import com.algotalk.userservice.repository.IUserWithdrawMapper;
import com.algotalk.userservice.service.IRefreshTokenService;
import com.algotalk.userservice.service.IUserWithdrawService;
import com.algotalk.userservice.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.algotalk.userservice.exception.UserErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWithdrawService implements IUserWithdrawService {
    private final IUserUpdateMapper userUpdateMapper;
    private final IUserWithdrawMapper userWithdrawMapper;
    private final PasswordEncoder passwordEncoder;
    private final IRefreshTokenService refreshTokenService;

    @Transactional
    @Override
    public int unlinkSocial(Long userId, String provider) throws Exception {
        log.info("{}.unlinkSocial Start!", this.getClass().getName());

        // 1. provider 유효한지 확인
        String normalizedProvider = CmmUtil.nvl(provider).strip().toUpperCase();
        LoginType loginType = resolveSocialLoginType(normalizedProvider);
        if (loginType == null) {
            log.warn("지원하지않는 provider: {}", provider);
            throw new BusinessException(OAUTH2_PROVIDER_NOT_SUPPORTED);
        }

        // 2. 사용자 조회
        UserInfoCommand rCommand = userUpdateMapper.getUserInfoByUserId(UserInfoCommand.builder().userId(userId).build());
        if (rCommand == null) {
            log.warn("사용자 정보가 존재하지 않습니다. userId: {}", userId);
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 3. 연결된 소셜 계정 수 확인
        int socialCount = userWithdrawMapper.getSocialAccountCountByUserId(UserInfoCommand.builder().userId(userId).build());

        // 4. 소셜 계정 1개로만 가입된 경우 마지막 로그인 수단 해제 방지
        if (socialCount == 1 && "N".equals(CmmUtil.nvl(rCommand.getPasswordSetYn(), "Y"))) {
            log.warn("마지막 로그인 수단 해제 불가능");
            throw new BusinessException(LAST_USER_METHOD);
        }

        // 5. 소셜계정 연결 해제(물리적으로 삭제)
        int res = userWithdrawMapper.deleteSocialAccountByProvider(
                UserInfoCommand.builder()
                        .userId(userId)
                        .provider(loginType.getProvider())
                        .build()
        );

        if (res != 1) {
            log.info("소셜 연결 해제 실패. userId: {}", userId);
            throw new BusinessException(SOCIAL_NOT_LINKED);
        }

        log.info("{}.unlinkSocial End!", this.getClass().getName());
        return 1;
    }

    @Transactional
    @Override
    public int withdraw(Long userId, WithdrawRequestDTO pDTO) throws Exception {
        log.info("{}.withdraw Start!", this.getClass().getName());

        // 1. 사용자 정보 조회
        UserInfoCommand rCommand = userUpdateMapper.getUserInfoByUserId(UserInfoCommand.builder().userId(userId).build());
        if (rCommand == null) {
            log.warn("사용자 정보가 존재하지 않습니다. userId: {}", userId);
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 2. PASSWORD_SET_YN = 'Y'이면 현재 비밀번호 검증(로그인 계정이 있음)
        String passwordSetYn = CmmUtil.nvl(rCommand.getPasswordSetYn(), "Y");
        if ("Y".equals(passwordSetYn)) {
            String currentPassword = pDTO == null ? "" : CmmUtil.nvl(pDTO.currentPassword());
            if (currentPassword.isBlank()) {
                log.warn("현재 비밀번호가 입력되지 않았습니다. userId: {}", userId);
                throw new BusinessException(WITHDRAW_PASSWORD_REQUIRED);
            }
            if (!passwordEncoder.matches(currentPassword, rCommand.getPassword())) {
                log.warn("현재 비밀번호가 일치하지 않습니다. userId: {}", userId);
                throw new BusinessException(CUR_PASSWORD_MISMATCH);
            }
        }

        // 3. USERS 탈퇴상태로 변경(soft delete)
        int res = userWithdrawMapper.withdrawUser(UserInfoCommand.builder().userId(userId).build());
        if (res != 1) {
            log.error("회원 탈퇴 처리 실패 userId: {}", userId);
            throw new BusinessException(WITHDRAW_FAIL);
        }

        // 4.연결된 소셜 계정 전부 삭제
        res = userWithdrawMapper.deleteAllSocialAccountsByUserId(UserInfoCommand.builder().userId(userId).build());
        if (res != 1) {
            log.error("회원 탈퇴 처리 실패 userId: {}", userId);
            throw new BusinessException(WITHDRAW_FAIL);
        }

        // 5. Redis에서 RT 삭제
        refreshTokenService.deleteRefreshToken(userId);

        log.info("{}.withdraw End!", this.getClass().getName());
        return 1;
    }

    // 소셜 타입 반환
    private LoginType resolveSocialLoginType(String provider) {
        for (LoginType type : LoginType.values()) {
            // 일반로그인은 제외
            if (type == LoginType.BASIC) {
                continue;
            }
            if (type.getProvider().equalsIgnoreCase(provider)) {
                return type;
            }
        }
        return null;
    }
}
