package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.EmailCheckRequestDTO;
import com.algotalk.userservice.dto.request.EmailSendRequestDTO;
import com.algotalk.userservice.dto.request.EmailVerifyRequestDTO;

public interface IEmailService {
    /**
     * 이메일 인증번호 발송
     * 6자리 인증번호 생성 -> Redis에 3분간 저장 -> 이메일 발송
     * @param pDTO
     * @throws Exception
     */
    void sendEmailVerificationCode(EmailSendRequestDTO pDTO) throws Exception;

    /**
     * 이메일 인증번호 확인
     * Redis에서 조회한 다음 입력값과 비교
     * 일치하면 인증완료 플래그 저장(30분 유효), Redis에서 인증번호 삭제
     * @param pDTO
     * @throws Exception
     */
    void verifyEmailCode(EmailVerifyRequestDTO pDTO) throws Exception;

    /**
     * 이메일 인증 완료 여부 확인
     * 회원가입 최종 INSERT 전에 이메일 인증 완료 여부 검증
     * @param email
     * @return
     * @throws Exception
     */
    boolean isEmailVerified(String email) throws Exception;
}
