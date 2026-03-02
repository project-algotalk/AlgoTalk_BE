package com.algotalk.userservice.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class LoginLogEntity {
    private Long logId;
    private Long userId;
    private String triedLoginId;
    private String loginType;
    private String result;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
