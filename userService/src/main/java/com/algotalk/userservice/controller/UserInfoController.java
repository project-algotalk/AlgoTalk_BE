package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.UserInfoRequestDTO;
import com.algotalk.userservice.dto.response.UserInfoResponseDTO;
import com.algotalk.userservice.service.impl.UserInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user/v1/info")
@RequiredArgsConstructor
public class UserInfoController {

    private final UserInfoService userInfoService;

    @PostMapping("/nickname")
    public ResponseEntity<ApiResponse<UserInfoResponseDTO>> getNicknameByUserId(
            @RequestBody UserInfoRequestDTO pDTO
    ) {
        log.info("{}.getUserInfo Start!", this.getClass().getName());

        UserInfoResponseDTO rDTO = userInfoService.getNicknameByUserId(
                UserInfoCommand.builder()
                        .userId(pDTO.userId())
                        .build()
        );

        log.info("{}.getUserInfo End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }
}
