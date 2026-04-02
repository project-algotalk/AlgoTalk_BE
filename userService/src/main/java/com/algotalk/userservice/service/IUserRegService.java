package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.SignUpRequestDTO;
import com.algotalk.userservice.dto.response.SignUpResponseDTO;

public interface IUserRegService {
    /**
     * loginId 중복 확인(중복이면 true)
     * @param pCommand
     * @return
     * @throws Exception
     */
    boolean isLoginIdDuplicated(UserInfoCommand pCommand) throws Exception;

    /**
     * nickname 중복 확인(중복이면 true)
     * @param pCommand
     * @return
     * @throws Exception
     */
    boolean isNicknameDuplicated(UserInfoCommand pCommand) throws Exception;

    /**
     * email 중복 확인(중복이면 true)
     * @param pCommand
     * @return
     * @throws Exception
     */
    boolean isEmailDuplicated(UserInfoCommand pCommand) throws Exception;

    /**
     * loginId 중복 여부 검증(중복이면 예외 발생)
     * @param pDTO
     * @throws Exception
     */
    void validateLoginIdUnique(SignUpRequestDTO pDTO) throws Exception;

    /**
     * nickname 중복 여부 검증(중복이면 예외 발생)
     * @param pDTO
     * @throws Exception
     */
    void validateNicknameUnique(SignUpRequestDTO pDTO) throws Exception;

    /**
     * email 중복 여부 검증(중복이면 예외 발생)
     * @param pDTO
     * @throws Exception
     */
    void validateEmailUnique(SignUpRequestDTO pDTO) throws Exception;

    /**
     * 회원 가입 처리
     * @param pDTO
     * @return SignUpResponseDTO (가입 성공 시 가입된 사용자 정보 반환, 실패 시 예외 발생)
     * @throws Exception
     */
    SignUpResponseDTO insertUser(SignUpRequestDTO pDTO) throws Exception;
}
