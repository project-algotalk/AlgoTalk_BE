package com.algotalk.userservice.dto.command;

import com.algotalk.common.pagination.Pagination;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private String keyword;
    private Pagination pagination;
}
