package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.SignUpRequestDTO;
import com.algotalk.userservice.dto.response.SignUpResponseDTO;

public interface IUserRegService {
    /**
     * loginId 중복 확인
     * <p>
     * 구현체에서는 loginId 가 중복인 경우 비즈니스 예외(예: BusinessException)를 발생시키고,
     * 중복이 아닌 정상 케이스에서는 false 를 반환합니다.
     *
     * @param pDTO 중복 여부를 검사할 loginId 정보를 포함한 요청 DTO
     * @return 중복이 아닐 때 false (구현체에 따라 확장 가능)
     * @throws Exception loginId 가 중복이거나 검사 과정에서 오류가 발생한 경우
     */
    boolean isLoginIdDuplicated(SignUpRequestDTO pDTO) throws Exception;

    /**
     * nickname 중복 확인
     * <p>
     * 구현체에서는 nickname 이 중복인 경우 비즈니스 예외(예: BusinessException)를 발생시키고,
     * 중복이 아닌 정상 케이스에서는 false 를 반환합니다.
     *
     * @param pDTO 중복 여부를 검사할 nickname 정보를 포함한 요청 DTO
     * @return 중복이 아닐 때 false (구현체에 따라 확장 가능)
     * @throws Exception nickname 이 중복이거나 검사 과정에서 오류가 발생한 경우
     */
    boolean isNicknameDuplicated(SignUpRequestDTO pDTO) throws Exception;

    /**
     * email 중복 확인
     * <p>
     * 구현체에서는 email 이 중복인 경우 비즈니스 예외(예: BusinessException)를 발생시키고,
     * 중복이 아닌 정상 케이스에서는 false 를 반환합니다.
     *
     * @param pDTO 중복 여부를 검사할 email 정보를 포함한 요청 DTO
     * @return 중복이 아닐 때 false (구현체에 따라 확장 가능)
     * @throws Exception email 이 중복이거나 검사 과정에서 오류가 발생한 경우
     */
    boolean isEmailDuplicated(SignUpRequestDTO pDTO) throws Exception;

    /**
     * 회원 가입 처리
     * @param pDTO
     * @return SignUpResponseDTO (가입 성공 시 가입된 사용자 정보 반환, 실패 시 예외 발생)
     * @throws Exception
     */
    SignUpResponseDTO insertUser(SignUpRequestDTO pDTO) throws Exception;
}
