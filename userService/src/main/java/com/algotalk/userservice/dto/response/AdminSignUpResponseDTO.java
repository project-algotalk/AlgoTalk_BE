package com.algotalk.userservice.dto.response;

// 가입 응답에 대한 DTO
public record AdminSignUpResponseDTO(
        Long userId,
        String approvalStatus // 승인 상태
) {
}