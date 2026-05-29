package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.response.UserInfoResponseDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IUserInfoMapper;
import com.algotalk.userservice.service.IUserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.algotalk.userservice.exception.UserErrorCode.INTERNAL_ERROR;
import static com.algotalk.userservice.exception.UserErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoService implements IUserInfoService {

    private final IUserInfoMapper userInfoMapper;

    @Override
    public UserInfoResponseDTO getNicknameByUserId(UserInfoCommand pCommand) {
        log.info("{}.getNicknameByUserId Start!", this.getClass().getName());

        UserInfoCommand rCommand = userInfoMapper.getNicknameByUserId(pCommand);

        // 존재하지 않는 userId
        if (rCommand == null) {
            throw new BusinessException(USER_NOT_FOUND);
        }

        UserInfoResponseDTO rDTO = UserInfoResponseDTO.builder()
                .userId(pCommand.getUserId())
                .nickname(rCommand.getNickname())
                .build();

        log.info("{}.getNicknameByUserId End!", this.getClass().getName());
        return rDTO;
    }
}
