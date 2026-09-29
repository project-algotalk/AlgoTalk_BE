package com.algotalk.userservice.dto.command;

import com.algotalk.common.pagination.Pagination;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminAccountCommand {
    private Long userId; // PK
    private String loginId;
    private String nickname;
    private String name;
    private String approvalStatus;
    private String adminGrade;
    private String role;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String rejectReason;
    private LocalDateTime createdAt;
    private Long totalCount;
    private Pagination pagination;
}