package com.algotalk.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenReissueResponseDTO(
        String accessToken, // 새로 발급된 Access Token
        String tokenType,
        Long expiresIn
) {
}
