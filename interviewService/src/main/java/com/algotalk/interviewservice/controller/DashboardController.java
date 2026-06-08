package com.algotalk.interviewservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.interviewservice.dto.request.DashboardRequestDTO;
import com.algotalk.interviewservice.dto.response.DashboardResponseDTO;
import com.algotalk.interviewservice.service.IDashboardService;
import jakarta.validation.Valid;
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
            @Valid @ModelAttribute DashboardRequestDTO rDTO
    ) {
        log.info("{}.getDashboard Start!", this.getClass().getName());
        DashboardResponseDTO responseDTO = dashboardService.getDashboard(userId, rDTO.toPagination());
        log.info("{}.getDashboard End!", this.getClass().getName());
        return ResponseEntity.ok(ApiResponse.ok(responseDTO));
    }
}