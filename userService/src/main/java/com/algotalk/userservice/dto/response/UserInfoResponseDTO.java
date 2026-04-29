package com.algotalk.userservice.dto.response;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.util.CmmUtil;
import com.algotalk.userservice.util.EncryptUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.Collections;
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
    public static UserInfoResponseDTO from(UserInfoCommand pCommand) throws Exception {

        String role = pCommand.getRole();

        List<String> roles = (role != null || !role.isBlank()) ?
                Collections.singletonList(role) : Collections.emptyList();

        return UserInfoResponseDTO.builder()
                .userId(pCommand.getUserId())
                .loginId(pCommand.getLoginId())
                .nickname(pCommand.getNickname())
                .name(pCommand.getName())
                .roles(roles)
                .email(EncryptUtil.decAES128CBC(CmmUtil.nvl(pCommand.getEmail())))
                .profileImgUrl(pCommand.getProfileImgUrl())
                .addr1(pCommand.getAddr1())
                .addr2(pCommand.getAddr2())
                .build();
    }
}