package com.algotalk.userservice.dto.response;

import java.time.LocalDateTime;

public record AdminStatusResponseDTO(
        Long userId,
        String approvalStatus,
        String adminGrade,
        String adminGradeLabel,
        LocalDateTime approvedAt,
        String rejectReason
) {
}