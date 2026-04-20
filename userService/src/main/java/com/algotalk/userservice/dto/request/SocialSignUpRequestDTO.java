package com.algotalk.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.List;

@Builder
public record SocialSignUpRequestDTO(
        // Redis 임시 토큰 (provider, providerId, email, name 조회용)
        @NotBlank(message = "임시 토큰이 없습니다.")
        String tempToken,

        // TB: USERS
        String nickname,

        String addr1,
        String addr2,

        // 목표 직무 (최대 3개)
        @Size(max = 3, message = "목표 직무는 최대 3개까지 입력 가능합니다.")
        List<TargetJobRequestDTO> targetJobs,

        // 경력 정보
        List<EmploymentRequestDTO> employments
) {
    // 닉네임 미입력 시 null 반환 (name은 Redis에서 가져오므로 Service에서 처리)
    public String resolvedNickname(String name) {
        return (nickname == null || nickname.isBlank()) ? name : nickname;
    }
}
