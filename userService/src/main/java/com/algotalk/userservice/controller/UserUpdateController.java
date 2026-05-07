package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.request.UpdateAddrRequestDTO;
import com.algotalk.userservice.dto.request.UpdateNameRequestDTO;
import com.algotalk.userservice.dto.request.UpdateNicknameRequestDTO;
import com.algotalk.userservice.dto.request.UpdatePasswordRequestDTO;
import com.algotalk.userservice.service.IUserUpdateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(value = "/mypage/v1")
@RequiredArgsConstructor
public class UserUpdateController {

    private final IUserUpdateService userUpdateService;

    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePasswordRequestDTO pDTO
    ) throws Exception {
        log.info("{}.updatePassword Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());

        userUpdateService.updatePassword(userId, pDTO);

        log.info("{}.updatePassword End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PostMapping("/update-nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateNicknameRequestDTO pDTO
    ) throws Exception {
        log.info("{}.updateNickname Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());

        userUpdateService.updateNickname(userId, pDTO);

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

        userUpdateService.updateName(userId, pDTO);

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

        userUpdateService.updateAddr(userId, pDTO);

        log.info("{}.updateAddr End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}

