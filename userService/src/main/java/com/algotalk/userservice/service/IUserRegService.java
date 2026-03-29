package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.SignUpRequestDTO;

public interface IUserRegService {
    /**
     * loginId 중복 확인(중복이면 true)
     * @param rCommand
     * @return
     * @throws Exception
     */
    boolean isLoginIdDuplicated(UserInfoCommand rCommand) throws Exception;

    /**
     * nickname 중복 확인(중복이면 true)
     * @param rCommand
     * @return
     * @throws Exception
     */
    boolean isNicknameDuplicated(UserInfoCommand rCommand) throws Exception;

    /**
     * email 중복 확인(중복이면 true)
     * @param rCommand
     * @return
     * @throws Exception
     */
    boolean isEmailDuplicated(UserInfoCommand rCommand) throws Exception;

    /**
     * 회원 가입 처리
     * @param rDTO
     * @return SignUpRequestDTO (가입 성공 시 가입된 사용자 정보 반환, 실패 시 예외 발생)
     * @throws Exception
     */
    SignUpRequestDTO insertUser(SignUpRequestDTO rDTO) throws Exception;
}
