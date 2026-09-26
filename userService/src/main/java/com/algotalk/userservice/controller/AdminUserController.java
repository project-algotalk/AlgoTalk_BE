package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.service.IAdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/v1/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final IAdminUserService adminUserService;

    @PatchMapping("/{userId}/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt
    ) throws Exception {

        adminUserService.unlockUser(userId, Long.valueOf(jwt.getSubject()));

        return ResponseEntity.ok(ApiResponse.ok());
    }
}