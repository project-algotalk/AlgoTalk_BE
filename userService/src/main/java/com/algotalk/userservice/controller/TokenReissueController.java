package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.response.TokenReissueResponseDTO;
import com.algotalk.userservice.service.ITokenReissueService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user/v1")
@RequiredArgsConstructor
public class TokenReissueController {

    private final ITokenReissueService tokenReissueService;

    @PostMapping("/token/reissue")
    public ResponseEntity<ApiResponse<TokenReissueResponseDTO>> reissueToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws Exception {
        log.info("{}.reissueToken Start!", this.getClass().getName());

        TokenReissueResponseDTO rDTO = tokenReissueService.reissueToken(request, response);

        log.info("{}.reissueToken End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }
}
