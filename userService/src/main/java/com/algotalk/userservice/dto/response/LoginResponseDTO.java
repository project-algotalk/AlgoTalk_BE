package com.algotalk.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * 로그인 성공 시 반환되는 DTO
 * @param accessToken
 * @param tokenType
 * @param userId
 * @param nickname
 * @param role
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponseDTO(
        String accessToken,
//        String refreshToken,
        String tokenType, // "Bearer"
        String userId,
        String nickname,
        String role
) {
}
