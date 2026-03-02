package com.algotalk.userservice.dto.response;

import com.algotalk.userservice.domain.entity.UserEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

/**
 * 마이페이지 등에서 사용자 정보 조회 시 반환하는 내용
 * @param userId
 * @param nickname
 * @param name
 * @param role
 * @param profileImgUrl
 * @param addr1
 * @param addr2
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserInfoResponseDTO(
        String userId,
        String email,
        String nickname,
        String name,
        String role,
        String profileImgUrl,
        String addr1,
        String addr2
) {
    public static UserInfoResponseDTO from(UserEntity entity) {
        return UserInfoResponseDTO.builder()
                .userId(String.valueOf(entity.getUserId()))
                .nickname(entity.getNickname())
                .name(entity.getName())
                .name(entity.getEmail())
                .role(entity.getRole().getValue())
                .profileImgUrl(entity.getProfileImgUrl())
                .addr1(entity.getAddr1())
                .addr2(entity.getAddr2())
                .build();
    }
}