package com.algotalk.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * 로그인 성공 시 반환되는 DTO
 * @param userId
 * @param loginId
 * @param nickname
 * @param roles
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponseDTO(
        Long userId,
        String loginId,
        String nickname,
        List<String> roles
) {
}
