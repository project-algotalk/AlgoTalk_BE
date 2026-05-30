package com.algotalk.communityservice.dto.request;

import lombok.Builder;

@Builder
public record UserInfoRequestDTO(
        Long userId
) {}