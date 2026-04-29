package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.UpdatePasswordRequestDTO;
import com.algotalk.userservice.repository.IUserUpdateMapper;
import com.algotalk.userservice.service.IUserUpdateService;
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
}