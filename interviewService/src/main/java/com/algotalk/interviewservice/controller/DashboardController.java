package com.algotalk.interviewservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.interviewservice.dto.response.DashboardResponseDTO;
import com.algotalk.interviewservice.service.IDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/interview/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final IDashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<DashboardResponseDTO>> getDashboard(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "5") Integer size
    ) {
        log.info("{}.getDashboard Start!", this.getClass().getName());
        DashboardResponseDTO rDTO = dashboardService.getDashboard(userId, page, size);
        log.info("{}.getDashboard End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(rDTO));
    }
}