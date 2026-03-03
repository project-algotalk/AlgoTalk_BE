package com.algotalk.userservice.domain.entity;

import com.algotalk.userservice.auth.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@Getter
public class UserEntity {
    private Long userId;
    private String nickname;
    private String name;
    private String email;
    private UserRole role;
    private String profileImgUrl;
    private String addr1;
    private String addr2;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
