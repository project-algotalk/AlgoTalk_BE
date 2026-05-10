package com.algotalk.userservice.dto.request;

import lombok.Builder;

@Builder
public record WithdrawRequestDTO(
        String currentPassword
) {
}