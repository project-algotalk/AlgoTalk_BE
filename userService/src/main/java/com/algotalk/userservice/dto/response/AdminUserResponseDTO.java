package com.algotalk.userservice.dto.response;

import java.time.LocalDateTime;

public record AdminUserResponseDTO(
        String loginId,
        String nickname,
        String name,
        LocalDateTime createdAt,
        boolean locked
) {
}
