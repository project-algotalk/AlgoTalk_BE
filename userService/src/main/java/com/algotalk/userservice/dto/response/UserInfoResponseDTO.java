package com.algotalk.userservice.dto.response;

import com.algotalk.userservice.domain.entity.UserEntity;
import com.algotalk.userservice.domain.entity.UserRolesEntity;
import com.algotalk.userservice.util.CmmUtil;
import com.algotalk.userservice.util.EncryptUtil;
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
        String email,
        String nickname,
        String name,

        List<String> roles,

        String profileImgUrl,
        String addr1,
        String addr2
) {
    public static UserInfoResponseDTO from(UserEntity entity) throws Exception {

        List<String> roles = entity.getRoles()
                .stream()
                .map(UserRolesEntity::getRole)
                .toList();

        return UserInfoResponseDTO.builder()
                .userId(entity.getUserId())
                .nickname(entity.getNickname())
                .name(entity.getName())
                .roles(roles)
                .email(EncryptUtil.decAES128CBC(CmmUtil.nvl(entity.getEmail())))
                .profileImgUrl(entity.getProfileImgUrl())
                .addr1(entity.getAddr1())
                .addr2(entity.getAddr2())
                .build();
    }
}