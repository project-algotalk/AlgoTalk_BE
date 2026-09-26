package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.request.AdminUserListRequestDTO;
import com.algotalk.userservice.dto.request.AdminUserUnlockRequestDTO;
import com.algotalk.userservice.dto.response.AdminUserPageResponseDTO;
import com.algotalk.userservice.service.IAdminUserService;
import jakarta.validation.Valid;
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

    @GetMapping
    public ResponseEntity<ApiResponse<AdminUserPageResponseDTO>> getUsers(
            @Valid @ModelAttribute AdminUserListRequestDTO request
    ) throws Exception {
        return ResponseEntity.ok(
                ApiResponse.ok(adminUserService.getUsers(request))
        );
    }

    @PatchMapping("/unlock")
    public ResponseEntity<ApiResponse<Void>> unlockUser(
            @Valid @RequestBody AdminUserUnlockRequestDTO request,
            @AuthenticationPrincipal Jwt jwt
    ) throws Exception {
        adminUserService.unlockUser(
                request.loginId(),
                Long.valueOf(jwt.getSubject())
        );

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
