package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.request.LoginRequestDTO;
import com.algotalk.userservice.dto.response.LoginResponseDTO;
import com.algotalk.userservice.service.IUserLoginService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user/v1")
@RequiredArgsConstructor
public class UserLoginController {

    private final IUserLoginService userLoginService;

    /**
     * 사용자 로그인
     * @param pDTO
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(@Valid @RequestBody LoginRequestDTO pDTO,
                                                               HttpServletResponse response) throws Exception {
        log.info("{}.login Start!", this.getClass().getName());

        userLoginService.login(pDTO, response);

        log.info("{}.login End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> me(@AuthenticationPrincipal Jwt jwt) throws Exception {
        log.info("{}.me Start!", this.getClass().getName());

        LoginResponseDTO rDTO = LoginResponseDTO.builder()
                .userId(Long.valueOf(jwt.getSubject()))
                .loginId(jwt.getClaimAsString("loginId"))
                .nickname(jwt.getClaimAsString("nickname"))
                .roles(jwt.getClaimAsStringList("roles"))
                .build();

        log.info("{}.me End!",  this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }

    /**
     * 사용자 로그아웃
     * @param jwt
     * @param response
     * @return
     * @throws Exception
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Jwt jwt,
                                                    HttpServletResponse response) throws Exception {
        log.info("{}.logout Start!", this.getClass().getName());

        Long userId = Long.valueOf(jwt.getSubject());
        userLoginService.logout(userId, response);

        return ResponseEntity.ok(ApiResponse.ok());
    }
}
