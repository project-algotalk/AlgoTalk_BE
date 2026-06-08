package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.request.WithdrawRequestDTO;
import com.algotalk.userservice.service.IUserWithdrawService;
import com.algotalk.userservice.util.CmmUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping(value = "/mypage/v1")
@RequiredArgsConstructor
public class UserWithdrawController {

    private final IUserWithdrawService userWithdrawService;

    @Value("${cookie.access.name}")
    private String accessCookieName;

    @Value("${cookie.refresh.name}")
    private String refreshCookieName;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String sameSite;

    // 소셜 연결 해제
    @DeleteMapping("/social/{provider}")
    public ResponseEntity<ApiResponse<Void>> unlinkSocial(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("provider") String provider
    ) throws Exception {
        log.info("{}.unlinkSocial Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());
        userWithdrawService.unlinkSocial(userId, provider);

        log.info("{}.unlinkSocial End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    // 회원 탈퇴
    @DeleteMapping("/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody(required = false) WithdrawRequestDTO pDTO,
            HttpServletResponse response
    ) throws Exception {
        log.info("{}.withdraw Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());
        userWithdrawService.withdraw(userId, pDTO == null ? WithdrawRequestDTO.builder().build() : pDTO);

        expireCookie(accessCookieName, response);
        expireCookie(refreshCookieName, response);

        log.info("{}.withdraw End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    private void expireCookie(String cookieName, HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
