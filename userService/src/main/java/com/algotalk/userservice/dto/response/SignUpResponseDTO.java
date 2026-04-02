package com.algotalk.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * 회원 가입 성공 시 반환되는 DTO
 * @param userId
 * @param nickname
 * @param email
 * @param targetJobs
 * @param createdAt
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SignUpResponseDTO(
        Long userId,        // 사용자 ID
        String nickname,    // 사용자 닉네임
        String email,       // 사용자 이메일 (암호화된 상태)
        List<String> targetJobs, // 사용자 목표 직무 리스트
        String createdAt    // 사용자 가입 일시
) {
}
