package com.algotalk.userservice.dto.response;

import java.time.LocalDateTime;

// 관리자 승인 상태 조회 응답에 대한 DTO
public record AdminStatusResponseDTO(
        Long userId,
        String approvalStatus,
        String adminGrade, // 관리자 등급
        LocalDateTime approvedAt, // 승인 일시
        String rejectReason // 반려 사유
) {
}
