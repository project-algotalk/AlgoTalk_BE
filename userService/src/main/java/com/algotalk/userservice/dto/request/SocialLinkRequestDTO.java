package com.algotalk.userservice.dto.request;

import lombok.Builder;

@Builder
public record SocialLinkRequestDTO(
    Long userId,
    String provider,
    String providerId
) {
}
