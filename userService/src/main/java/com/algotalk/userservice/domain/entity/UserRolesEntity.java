package com.algotalk.userservice.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class UserRolesEntity {
    private Long userRoleId;
    private Long userId;
    private String role;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
