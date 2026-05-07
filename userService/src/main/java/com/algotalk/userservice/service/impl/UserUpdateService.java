package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.*;
import com.algotalk.userservice.dto.response.ExistsResponseDTO;
import com.algotalk.userservice.repository.IUserUpdateMapper;
import com.algotalk.userservice.service.IEmailService;
import com.algotalk.userservice.service.IUserUpdateService;
import com.algotalk.userservice.util.CmmUtil;
import com.algotalk.userservice.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.algotalk.userservice.exception.UserErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserUpdateService implements IUserUpdateService {

    private final IUserUpdateMapper userUpdateMapper;
    private final PasswordEncoder passwordEncoder;
    private final IEmailService emailService;

    @Transactional
    @Override
    public int updatePassword(Long userId, UpdatePasswordRequestDTO pDTO) throws Exception {
        log.info("{}.updatePassword Start!", this.getClass().getName());

        log.info("userId: {}", userId);
        int res = 0;

        // 1. 변경할 비밀번호가 일치하는지 검증
        boolean passwordConfirmed = pDTO.isPasswordConfirmed();
        if(!passwordConfirmed) {
            log.warn("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");
            throw new BusinessException(PASSWORD_MISMATCH);
        }

        UserInfoCommand pCommand = UserInfoCommand.builder()
                .userId(userId)
                .build();

        // 2. 현재 비밀번호 조회
        UserInfoCommand rCommand = userUpdateMapper.getUserInfoByUserId(pCommand);
        if(rCommand == null) {
            log.warn("사용자 정보가 존재하지 않습니다.");
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 3. 현재 비밀번호가 입력한 현재 비밀번호와 일치하는지
        boolean currentPasswordMatches = passwordEncoder.matches(pDTO.currentPassword(), rCommand.getPassword());
        if(!currentPasswordMatches) {
            log.warn("현재 비밀번호가 일치하지 않습니다.");
            throw new BusinessException(CUR_PASSWORD_MISMATCH);
        }

        // 4. 현재 비밀번호와 동일한 비밀번호로 변경하지 못하도록 검증
        if(passwordEncoder.matches(pDTO.newPassword(), rCommand.getPassword())) {
            log.warn("현재 비밀번호와 동일한 비밀번호로 변경할 수 없습니다.");
            throw new BusinessException(NOW_PASSWORD_SAME);
        }

        // 5. 비밀번호 변경
        int updateCount = userUpdateMapper.updatePassword(
                UserInfoCommand.builder()
                    .userId(userId)
                    .password(passwordEncoder.encode(pDTO.newPassword()))
                    .build()
        );

        if (updateCount != 1) {
            log.error("비밀번호 변경이 실패했습니다.");
            throw new BusinessException(PASSWORD_UPDATE_FAIL);
        }

        res = 1; // 성공적으로 변경된 경우 1 반환

        log.info("{}.updatePassword End!", this.getClass().getName());
        return res;
    }

    @Transactional
    @Override
    public int updateNickname(Long userId, UpdateNicknameRequestDTO pDTO) throws Exception {
        log.info("{}.updateNickname Start!", this.getClass().getName());

        log.info("userId: {}", userId);
        int res = 0;

        // 1. 닉네임 중복 검증
        ExistsResponseDTO rDTO = userUpdateMapper.getNicknameExists(pDTO);
        String existsYn = rDTO.existsYn();

        if(existsYn.equals("Y")) {
            log.warn("이미 사용 중인 닉네임입니다.");
            throw new BusinessException(DUPLICATE_NICKNAME);
        }

        // 2. 닉네임 변경
        res = userUpdateMapper.updateNickname(
                UserInfoCommand.builder()
                        .userId(userId)
                        .nickname(pDTO.nickname().strip())
                        .build());

        if(res != 1) {
            log.error("닉네임 변경이 실패했습니다.");
            throw new BusinessException(NICKNAME_UPDATE_FAIL);
        }

        log.info("{}.updateNickname End!", this.getClass().getName());
        return res;
    }

    @Transactional
    @Override
    public int updateName(Long userId, UpdateNameRequestDTO pDTO) throws Exception {
        log.info("{}.updateName Start!", this.getClass().getName());

        log.info("userId: {}", userId);
        int res = 0;

        // 1. 이름 변경
        res = userUpdateMapper.updateName(
                UserInfoCommand.builder()
                        .userId(userId)
                        .name(CmmUtil.nvl(pDTO.name()))
                        .build());

        if(res != 1) {
            log.error("이름 변경이 실패했습니다.");
            throw new BusinessException(NAME_UPDATE_FAIL);
        }

        log.info("{}.updateName End!", this.getClass().getName());
        return res;
    }

    @Transactional
    @Override
    public int updateAddr(Long userId, UpdateAddrRequestDTO pDTO) throws Exception {
        log.info("{}.updateAddr Start!", this.getClass().getName());

        log.info("userId: {}", userId);

        // 1. 주소 변경
        int res = userUpdateMapper.updateAddr(
                UserInfoCommand.builder()
                        .userId(userId)
                        .addr1(CmmUtil.nvl(pDTO.addr1()))
                        .addr2(CmmUtil.nvl(pDTO.addr2()))
                        .build());

        if(res != 1) {
            log.error("주소 변경이 실패했습니다.");
            throw new BusinessException(ADDR_UPDATE_FAIL);
        }

        log.info("{}.updateAddr End!", this.getClass().getName());
        return res;
    }

    @Override
    public void isEmailDuplicated(UpdateEmailRequestDTO pDTO) throws Exception {
        log.info("{}.isEmailDuplicated Start!", this.getClass().getName());

        // 1. 이메일 암호화
        UpdateEmailRequestDTO encDTO = UpdateEmailRequestDTO.builder()
                .email(EncryptUtil.encAES128CBC(pDTO.email().strip()))
                .build();

        // 2. DB 조회
        ExistsResponseDTO rDTO = userUpdateMapper.getEmailExists(encDTO);
        String existsYn = rDTO.existsYn();

        if(existsYn.equals("Y")) {
            log.warn("이미 사용 중인 이메일입니다.");
            throw new BusinessException(DUPLICATE_EMAIL);
        }

        log.info("{}.isEmailDuplicated End!", this.getClass().getName());
    }

    @Transactional
    @Override
    public int updateEmail(Long userId, UpdateEmailRequestDTO pDTO) throws Exception {
        log.info("{}.updateEmail Start!", this.getClass().getName());

        log.info("userId: {}", userId);

        // 1. 이메일 변경
        int res = userUpdateMapper.updateEmail(
                UserInfoCommand.builder()
                        .userId(userId)
                        .email(EncryptUtil.encAES128CBC(pDTO.email().strip()))
                        .build()
        );

        if(res != 1) {
            log.error("이메일 변경이 실패했습니다.");
            throw new BusinessException(EMAIL_UPDATE_FAIL);
        }

        log.info("{}.updateEmail End!", this.getClass().getName());
        return res;
    }
}