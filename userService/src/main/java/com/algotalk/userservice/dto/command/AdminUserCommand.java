package com.algotalk.userservice.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserCommand {

    private Long userId;
    private String loginId;
    private String nickname;
    private String name;
    private String email;
    private String passwordSetYn;
}