package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.client.CommunityFeignClient;
import com.algotalk.userservice.dto.request.*;
import com.algotalk.userservice.dto.response.*;
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
    private final CommunityFeignClient communityFeignClient;

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

    // 내가 작성한 게시글 목록
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<MyPostResponseDTO>>> getMyPosts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("{}.getMyPosts Start!", this.getClass().getName());
        Long userId = Long.valueOf(jwt.getSubject());
        ApiResponse<List<MyPostResponseDTO>> rDTO = communityFeignClient.getMyPosts(userId, page, size);
        log.info("{}.getMyPosts End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO.getData()));
    }

    // 내가 작성한 게시글 삭제
    @DeleteMapping("/posts")
    public ResponseEntity<ApiResponse<Void>> deleteMyPosts(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody List<Long> postIds
    ) {
        log.info("{}.deleteMyPosts Start!", this.getClass().getName());
        Long userId = Long.valueOf(jwt.getSubject());
        communityFeignClient.deleteMyPosts(userId, postIds);
        log.info("{}.deleteMyPosts End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 내가 작성한 댓글 목록
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<List<MyCommentResponseDTO>>> getMyComments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("{}.getMyComments Start!", this.getClass().getName());
        Long userId = Long.valueOf(jwt.getSubject());
        ApiResponse<List<MyCommentResponseDTO>> rDTO = communityFeignClient.getMyComments(userId, page, size);
        log.info("{}.getMyComments End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO.getData()));
    }

    // 내가 작성한 댓글 삭제
    @DeleteMapping("/comments")
    public ResponseEntity<ApiResponse<Void>> deleteMyComments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody List<Long> commentIds
    ) {
        log.info("{}.deleteMyComments Start!", this.getClass().getName());
        Long userId = Long.valueOf(jwt.getSubject());
        communityFeignClient.deleteMyComments(userId, commentIds);
        log.info("{}.deleteMyComments End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 내가 스크랩한 게시글 목록
    @GetMapping("/scraps")
    public ResponseEntity<ApiResponse<List<MyScrapResponseDTO>>> getMyScraps(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("{}.getMyScraps Start!", this.getClass().getName());
        Long userId = Long.valueOf(jwt.getSubject());
        ApiResponse<List<MyScrapResponseDTO>> rDTO = communityFeignClient.getMyScraps(userId, page, size);
        log.info("{}.getMyScraps End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO.getData()));
    }

    // 스크랩 취소
    @DeleteMapping("/scraps")
    public ResponseEntity<ApiResponse<Void>> deleteMyScraps(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody List<Long> postIds
    ) {
        log.info("{}.deleteMyScraps Start!", this.getClass().getName());
        Long userId = Long.valueOf(jwt.getSubject());
        communityFeignClient.deleteMyScraps(userId, postIds);
        log.info("{}.deleteMyScraps End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // 내가 좋아요한 게시글 목록
    @GetMapping("/likes")
    public ResponseEntity<ApiResponse<List<MyLikeResponseDTO>>> getMyLikes(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("{}.getMyLikes Start!", this.getClass().getName());
        Long userId = Long.valueOf(jwt.getSubject());
        ApiResponse<List<MyLikeResponseDTO>> rDTO = communityFeignClient.getMyLikes(userId, page, size);
        log.info("{}.getMyLikes End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO.getData()));
    }

    // 좋아요 취소
    @DeleteMapping("/likes")
    public ResponseEntity<ApiResponse<Void>> deleteMyLikes(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody List<Long> postIds
    ) {
        log.info("{}.deleteMyLikes Start!", this.getClass().getName());
        Long userId = Long.valueOf(jwt.getSubject());
        communityFeignClient.deleteMyLikes(userId, postIds);
        log.info("{}.deleteMyLikes End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}

