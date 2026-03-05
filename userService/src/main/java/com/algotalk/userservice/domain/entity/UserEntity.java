package com.algotalk.userservice.domain.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor
@Getter
public class UserEntity {
    private Long userId;
    private String nickname;
    private String name;
    private String email;
    private String profileImgUrl;
    private String addr1;
    private String addr2;

    private List<UserRolesEntity> roles; // resultMap에서 userId로 매핑된 UserRolesEntity 리스트

    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
}
