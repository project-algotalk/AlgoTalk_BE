package com.algotalk.userservice.dto.response;

// 관리자 대시보드 요약 응답에 대한 DTO
public record AdminDashboardSummaryResponseDTO(
        Long userCount,
        Long adminCount,
        Long pendingAdminRequestCount
) {
}