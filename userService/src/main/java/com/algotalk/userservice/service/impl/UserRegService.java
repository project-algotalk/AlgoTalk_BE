package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.SignUpRequestDTO;
import com.algotalk.userservice.service.IUserRegService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRegService implements IUserRegService {
    @Override
    public boolean isLoginIdDuplicated(UserInfoCommand rCommand) throws Exception {
        return false;
    }

    @Override
    public boolean isNicknameDuplicated(UserInfoCommand rCommand) throws Exception {
        return false;
    }

    @Override
    public boolean isEmailDuplicated(UserInfoCommand rCommand) throws Exception {
        return false;
    }

    @Override
    public SignUpRequestDTO insertUser(SignUpRequestDTO rDTO) throws Exception {
        return null;
    }
}
