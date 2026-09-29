package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.request.AdminApplicationListRequestDTO;
import com.algotalk.userservice.dto.request.AdminRejectRequestDTO;
import com.algotalk.userservice.dto.request.AdminSignUpRequestDTO;
import com.algotalk.userservice.dto.response.AdminApplicationResponseDTO;
import com.algotalk.userservice.dto.response.AdminDashboardSummaryResponseDTO;
import com.algotalk.userservice.dto.response.AdminSignUpResponseDTO;
import com.algotalk.userservice.dto.response.AdminStatusResponseDTO;
import com.algotalk.userservice.service.IAdminAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/v1")
@RequiredArgsConstructor
public class AdminAccountController {

    private final IAdminAccountService adminAccountService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AdminSignUpResponseDTO>> signUp(
            @Valid @RequestBody AdminSignUpRequestDTO request
    ) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "관리자 가입 승인 대기 중입니다.",
                        adminAccountService.signUp(request)
                ));
    }

    @GetMapping("/signup/status")
    public ResponseEntity<ApiResponse<AdminStatusResponseDTO>> getStatus(
            @AuthenticationPrincipal Jwt jwt
    ) throws Exception {
        Long userId = Long.valueOf(jwt.getSubject());

        return ResponseEntity.ok(
                ApiResponse.ok(adminAccountService.getStatus(userId))
        );
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<
            ApiResponse<AdminDashboardSummaryResponseDTO>
            > getDashboardSummary() throws Exception {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminAccountService.getDashboardSummary()
                )
        );
    }

    @GetMapping("/applications")
    public ResponseEntity<
            ApiResponse<List<AdminApplicationResponseDTO>>
            > getApplications(
            @Valid @ModelAttribute
            AdminApplicationListRequestDTO request
    ) throws Exception {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        adminAccountService.getApplications(request)
                )
        );
    }

    @PatchMapping("/applications/{userId}/approve")
    public ResponseEntity<ApiResponse<Void>> approve(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt
    ) throws Exception {
        adminAccountService.approve(
                userId,
                Long.valueOf(jwt.getSubject())
        );

        return ResponseEntity.ok(ApiResponse.ok());
    }

    @PatchMapping("/applications/{userId}/reject")
    public ResponseEntity<ApiResponse<Void>> reject(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AdminRejectRequestDTO request
    ) throws Exception {
        adminAccountService.reject(
                userId,
                Long.valueOf(jwt.getSubject()),
                request.reason()
        );

        return ResponseEntity.ok(ApiResponse.ok());
    }
}