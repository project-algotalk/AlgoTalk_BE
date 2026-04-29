package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.request.UpdatePasswordRequestDTO;
import com.algotalk.userservice.service.IUpdateUserService;
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

    private final IUpdateUserService updateUserService;

    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdatePasswordRequestDTO pDTO
    ) throws Exception {
        log.info("{}.updatePassword Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());

        updateUserService.updatePassword(userId, pDTO);

        log.info("{}.updatePassword End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }
}

