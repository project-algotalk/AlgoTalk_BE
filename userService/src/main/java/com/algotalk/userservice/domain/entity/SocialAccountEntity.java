package com.algotalk.userservice.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class SocialAccountEntity {
    private Long socialAccountId;
    private Long userId;
    private String providerCd;
    private String providerUid;
    private String socialEmail;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;
}