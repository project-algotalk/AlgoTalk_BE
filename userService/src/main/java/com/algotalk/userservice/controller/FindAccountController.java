package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.request.EmailVerifyRequestDTO;
import com.algotalk.userservice.dto.request.FindLoginIdRequestDTO;
import com.algotalk.userservice.dto.request.FindPasswordRequestDTO;
import com.algotalk.userservice.dto.request.ResetPasswordRequestDTO;
import com.algotalk.userservice.dto.response.UserInfoResponseDTO;
import com.algotalk.userservice.service.IEmailService;
import com.algotalk.userservice.service.IFindAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user/v1/find")
@RequiredArgsConstructor
public class FindAccountController {

    private final IFindAccountService findAccountService;
    private final IEmailService emailService;

    // 아이디 찾기 - 이메일 인증번호 발송
    @PostMapping("/loginId/email-code")
    public ResponseEntity<ApiResponse<Void>> sendFindLoginIdEmail(
            @Valid @RequestBody FindLoginIdRequestDTO pDTO) throws Exception {
        log.info("{}.sendFindLoginIdEmail Start!", this.getClass().getSimpleName());

        findAccountService.sendFindLoginIdEmail(pDTO);

        log.info("{}.sendFindLoginIdEmail End!", this.getClass().getSimpleName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 아이디 찾기 - 이메일 인증번호 확인
    @PostMapping("/loginId/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyFindLoginIdCode(
            @Valid @RequestBody EmailVerifyRequestDTO pDTO) throws Exception {
        log.info("{}.verifyFindLoginIdCode Start!", this.getClass().getSimpleName());

        emailService.verifyEmailCode(pDTO);

        log.info("{}.verifyFindLoginIdCode End!", this.getClass().getSimpleName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 아이디 찾기 - 아이디 조회
    @PostMapping("/loginId")
    public ResponseEntity<ApiResponse<UserInfoResponseDTO>> findLoginId(
            @Valid @RequestBody FindLoginIdRequestDTO pDTO) throws Exception {
        log.info("{}.findLoginId Start!", this.getClass().getSimpleName());

        UserInfoResponseDTO rDTO = findAccountService.findLoginId(pDTO);

        log.info("{}.findLoginId End!", this.getClass().getSimpleName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }

    // 비밀번호 찾기 - 이메일 인증번호 발송
    @PostMapping("/password/email-code")
    public ResponseEntity<ApiResponse<Void>> sendFindPasswordEmail(
            @Valid @RequestBody FindPasswordRequestDTO pDTO) throws Exception {
        log.info("{}.sendFindPasswordEmail Start!", this.getClass().getSimpleName());

        findAccountService.sendFindPasswordEmail(pDTO);

        log.info("{}.sendFindPasswordEmail End!", this.getClass().getSimpleName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 비밀번호 찾기 - 이메일 인증번호 확인
    @PostMapping("/password/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyFindPasswordCode(
            @Valid @RequestBody EmailVerifyRequestDTO pDTO) throws Exception {
        log.info("{}.verifyFindPasswordCode Start!", this.getClass().getSimpleName());

        emailService.verifyEmailCode(pDTO);

        log.info("{}.verifyFindPasswordCode End!", this.getClass().getSimpleName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 비밀번호 재설정
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequestDTO pDTO) throws Exception {
        log.info("{}.resetPassword Start!", this.getClass().getSimpleName());

        findAccountService.resetPassword(pDTO);

        log.info("{}.resetPassword End!", this.getClass().getSimpleName());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}