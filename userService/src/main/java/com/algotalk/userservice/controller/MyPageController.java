package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.request.*;
import com.algotalk.userservice.dto.response.MyPageResponseDTO;
import com.algotalk.userservice.dto.response.TargetJobInfoResponseDTO;
import com.algotalk.userservice.service.IEmailService;
import com.algotalk.userservice.service.ISocialLinkService;
import com.algotalk.userservice.service.IMypageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping(value = "/mypage/v1")
@RequiredArgsConstructor
public class MyPageController {

    private final IMypageService mypageService;
    private final IEmailService emailService;
    private final ISocialLinkService socialLinkService;

    @GetMapping
    public ResponseEntity<ApiResponse<MyPageResponseDTO>> getMyPage(
            @AuthenticationPrincipal Jwt jwt
    ) throws Exception {
        log.info("{}.getMypage Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());

        MyPageResponseDTO rDTO = mypageService.getMyPage(userId);

        log.info("{}.getMypage End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }

    @PostMapping("/update-loginId")
    public ResponseEntity<ApiResponse<Void>> updateLoginId(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateLoginIdRequestDTO pDTO
    ) throws Exception {
        log.info("{}.updateLoginId Start!", this.getClass().getName());
        mypageService.updateLoginId(Long.valueOf(jwt.getSubject()), pDTO);
        log.info("{}.updateLoginId End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePasswordRequestDTO pDTO
    ) throws Exception {
        log.info("{}.updatePassword Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());

        mypageService.updatePassword(userId, pDTO);

        log.info("{}.updatePassword End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SetPasswordRequestDTO pDTO
    ) throws Exception {
        log.info("{}.setPassword Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());
        mypageService.setPassword(userId, pDTO);

        log.info("{}.setPassword End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/update-nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateNicknameRequestDTO pDTO
    ) throws Exception {
        log.info("{}.updateNickname Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());

        mypageService.updateNickname(userId, pDTO);

        log.info("{}.updateNickname End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/update-name")
    public ResponseEntity<ApiResponse<Void>> updateName(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateNameRequestDTO pDTO
    ) throws Exception {
        log.info("{}.updateName Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());

        mypageService.updateName(userId, pDTO);

        log.info("{}.updateName End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/update-addr")
    public ResponseEntity<ApiResponse<Void>> updateAddr(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateAddrRequestDTO pDTO
    ) throws Exception {
        log.info("{}.updateAddr Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());

        mypageService.updateAddr(userId, pDTO);

        log.info("{}.updateAddr End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 이메일 인증번호 발송
    @PostMapping("/email-code")
    public ResponseEntity<ApiResponse<Void>> sendEmailVerificationCode(@Valid @RequestBody EmailSendRequestDTO pDTO) throws Exception {
        log.info("{}.sendEmailVerificationCode Start!", this.getClass().getName());
        log.info("email: {}", pDTO.email());

        // 이메일 중복 확인 로직 처리
        mypageService.isEmailDuplicated(UpdateEmailRequestDTO.builder()
                .email(pDTO.email())
                .build());

        // 이메일 인증번호 발송 로직 처리
        emailService.sendEmailVerificationCode(pDTO);

        log.info("{}.sendEmailVerificationCode End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 이메일 인증번호 확인
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Void>> verifyEmailCode(@Valid @RequestBody EmailVerifyRequestDTO pDTO) throws Exception {
        log.info("{}.verifyEmailCode Start!", this.getClass().getName());

        // 이메일 인증번호 확인 로직 처리
        emailService.verifyEmailCode(pDTO);

        log.info("{}.verifyEmailCode End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/update-email")
    public ResponseEntity<ApiResponse<Void>> updateEmail(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateEmailRequestDTO pDTO
    ) throws Exception {
        log.info("{}.updateEmail Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());
        mypageService.updateEmail(userId, pDTO);

        log.info("{}.updateEmail End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/social/link/{provider}")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> issueLinkToken(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String provider
    ) throws Exception {
        log.info("{}.issueLinkToken Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());
        String linkToken = socialLinkService.issueLinkToken(SocialLinkRequestDTO.builder()
                .userId(userId)
                .provider(provider)
                .build());

        log.info("{}.issueLinkToken End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(java.util.Map.of("linkToken", linkToken)));
    }

    @PostMapping("/target-jobs")
    public ResponseEntity<ApiResponse<Void>> updateTargetJobs(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody List<TargetJobRequestDTO> pDTO
    ) throws Exception {
        log.info("{}.updateTargetJobs Start!", this.getClass().getName());

        mypageService.updateTargetJobs(Long.valueOf(jwt.getSubject()), pDTO);

        log.info("{}.updateTargetJobs End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/employments")
    public ResponseEntity<ApiResponse<Void>> updateEmployments(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody List<EmploymentRequestDTO> pDTO
    ) throws Exception {
        log.info("{}.updateEmployments Start!", this.getClass().getName());

        mypageService.updateEmployments(Long.valueOf(jwt.getSubject()), pDTO);

        log.info("{}.updateEmployments End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/target-jobs")
    public ResponseEntity<ApiResponse<List<TargetJobInfoResponseDTO>>> getTargetJobs(
            @AuthenticationPrincipal Jwt jwt
    ) throws Exception {
        log.info("{}.getTargetJobs Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());
        List<TargetJobInfoResponseDTO> rList = mypageService.getTargetJobs(userId);

        log.info("{}.getTargetJobs End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rList));
    }
}

