package com.algotalk.communityservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

/**
 * 마이페이지 등에서 사용자 정보 조회 시 반환하는 내용
 * @param userId
 * @param nickname
 * @param name
 * @param roles
 * @param profileImgUrl
 * @param addr1
 * @param addr2
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfoResponseDTO(
        Long userId,
        String loginId,
        String email,
        String nickname,
        String name,

        List<String> roles,

        String profileImgUrl,
        String addr1,
        String addr2
) {
}