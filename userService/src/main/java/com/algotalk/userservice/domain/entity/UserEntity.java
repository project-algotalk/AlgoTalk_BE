package com.algotalk.userservice.domain.entity;

import com.algotalk.userservice.auth.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class UserEntity {
    private Long userId;
    private String nickname;
    private String name;
    private UserRole role;
    private String profileImgUrl;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
