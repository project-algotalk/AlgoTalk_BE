package com.algotalk.userservice.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class UserCredentialEntity {
    private Long userId;
    private String loginId;
    private String password;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
