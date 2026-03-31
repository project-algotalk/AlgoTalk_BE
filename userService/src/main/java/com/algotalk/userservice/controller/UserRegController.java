package com.algotalk.userservice.controller;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.common.exception.ErrorCode;
import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.request.SignUpRequestDTO;
import com.algotalk.userservice.dto.response.SignUpResponseDTO;
import com.algotalk.userservice.service.impl.UserRegService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RequestMapping(value = "/user/v1")
@Controller
@RequiredArgsConstructor
public class UserRegController {

    private final UserRegService userRegService;

    // 회원가입 페이지 진입
    @GetMapping("/signup")
    public String signup() {
        log.info("UserRegController.register Start!");

        // 회원가입 페이지로 이동
        log.info("UserRegController.register End!");
        return "signup"; // 리액트로 프론트 구현 시 "signup" 대신 프론트 URL로 변경
    }

    // 목표직무 페이지 진입
    @GetMapping("/target-job")
    public String targetJob() {
        log.info("UserRegController.targetJob Start!");

        // 목표직무 페이지로 이동
        log.info("UserRegController.targetJob End!");
        return "target-job"; // 리액트로 프론트 구현 시 "target-job" 대신 프론트 URL로 변경
    }

    // 재직이력 페이지 진입
    @GetMapping("/employment")
    public String employment() {
        log.info("UserRegController.employment Start!");

        // 재직이력 페이지로 이동
        log.info("UserRegController.employment End!");
        return "employment"; // 리액트로 프론트 구현 시 "employment" 대신 프론트 URL로 변경
    }

    // TODO: 아이디 중복, 이메일 중복, 이메일 인증번호 발송/확인, 회원가입 로직 완성
    // 아이디 중복 확인
    @PostMapping("reg/check/loginId")
    public ResponseEntity<ApiResponse<Boolean>> checkLoginId(@RequestParam SignUpRequestDTO pDTO) throws Exception {
        log.info("UserRegController.checkLoginId Start!");

        // 아이디 중복 확인 로직 처리
        boolean loginIdDuplicated = userRegService.isLoginIdDuplicated(pDTO);

        log.info("UserRegController.checkLoginId End!");
        return ResponseEntity.ok(ApiResponse.ok(loginIdDuplicated));
    }

    // 이메일 중복 확인
    @PostMapping("reg/check/email")
    public ResponseEntity<ApiResponse<Boolean>> checkEmail(@RequestParam SignUpRequestDTO pDTO) throws Exception {
        log.info("UserRegController.checkEmail Start!");

        // 이메일 중복 확인 로직 처리
        boolean emailDuplicated = userRegService.isEmailDuplicated(pDTO);

        log.info("UserRegController.checkEmail End!");
        return ResponseEntity.ok(ApiResponse.ok(emailDuplicated));
    }

    // 닉네임 중복 확인
    @PostMapping("reg/check/nickname")
    public ResponseEntity<ApiResponse<Boolean>>  checkNickname(@RequestParam SignUpRequestDTO pDTO) throws Exception {
        log.info("UserRegController.checkNickname Start!");

        // 닉네임 중복 확인 로직 처리
        boolean nicknameDuplicated = userRegService.isNicknameDuplicated(pDTO);

        log.info("UserRegController.checkNickname End!");
        return ResponseEntity.ok(ApiResponse.ok(nicknameDuplicated));
    }

    // 이메일 인증번호 발송
    @PostMapping("reg/send/email-code")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerificationCode() throws Exception {
        log.info("UserRegController.sendEmailVerificationCode Start!");

        // 이메일 인증번호 발송 로직 처리

        log.info("UserRegController.sendEmailVerificationCode End!");
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 이메일 인증번호 확인
    @PostMapping("reg/check/email-code")
    public ResponseEntity<ApiResponse<Void>> verifyEmailCode() {
        log.info("UserRegController.verifyEmailCode Start!");

        // 이메일 인증번호 확인 로직 처리

        log.info("UserRegController.verifyEmailCode End!");
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponseDTO>> register(@RequestBody SignUpRequestDTO pDTO) throws Exception {
        log.info("UserRegController.register Start!");

        // 회원가입 로직 처리
        SignUpResponseDTO rDTO = userRegService.insertUser(pDTO);

        log.info("UserRegController.register End!");
        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }
}
