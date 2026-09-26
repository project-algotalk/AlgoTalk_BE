package com.algotalk.userservice.dto.response;

import java.time.LocalDateTime;

public record AdminApplicationResponseDTO(
        Long userId,
        String loginId,
        String nickname,
        String name,
        String approvalStatus,
        String adminGrade,
        String adminGradeLabel,
        LocalDateTime createdAt,
        Long totalCount
) {
}