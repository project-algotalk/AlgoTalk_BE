package com.algotalk.userservice.dto.response;

import java.time.LocalDateTime;

// 관리자 가입 신청 목록 응답
public record AdminApplicationResponseDTO(
        Long userId,
        String loginId,
        String nickname,
        String name,
        String approvalStatus,
        String adminGrade,
        LocalDateTime createdAt,
        Long totalCount
) {
}