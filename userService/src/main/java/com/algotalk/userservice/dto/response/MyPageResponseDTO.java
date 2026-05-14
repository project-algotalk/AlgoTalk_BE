package com.algotalk.userservice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class MyPageResponseDTO {
    // 기본 정보
    private Long userId;
    private String nickname;
    private String name;
    private String email;
    private String addr1;
    private String addr2;
    private LocalDateTime createdAt;

    // 로그인 정보
    private String loginId;
    private String passwordSetYn;

    // 소셜 계정 목록
    private List<String> socialProviders;

    // 목표 직무 목록
    private List<TargetJobInfoDTO> targetJobs;

    // 재직 이력 목록
    private List<EmploymentInfoDTO> employments;

    @Getter
    @Builder
    public static class TargetJobInfoDTO {
        private Long categoryId;
        private String categoryName;
        private LocalDate startDate;
        private LocalDate endDate;
    }

    @Getter
    @Builder
    public static class EmploymentInfoDTO {
        private String companyName;
        private Long categoryId;
        private String categoryName;
        private LocalDate startDate;
        private LocalDate endDate;
    }
}
