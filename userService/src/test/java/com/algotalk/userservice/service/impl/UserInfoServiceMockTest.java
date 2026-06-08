package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.response.UserInfoResponseDTO;
import com.algotalk.userservice.exception.UserErrorCode;
import com.algotalk.userservice.repository.IUserInfoMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserInfoServiceMockTest {

    @InjectMocks
    private UserInfoService userInfoService;

    @Mock
    private IUserInfoMapper userInfoMapper;

    @Test
    @DisplayName("닉네임 조회 성공")
    void getNicknameByUserId_success() throws Exception {
        // given
        UserInfoCommand pCommand = UserInfoCommand.builder()
                .userId(1L)
                .build();

        given(userInfoMapper.getNicknameByUserId(any()))
                .willReturn(
                        UserInfoCommand.builder()
                                .userId(1L)
                                .nickname("테스트닉네임")
                                .build()
                );

        // when
        UserInfoResponseDTO rDTO = userInfoService.getNicknameByUserId(pCommand);

        // then
        assertThat(rDTO).isNotNull();
        assertThat(rDTO.nickname()).isEqualTo("테스트닉네임");
        assertThat(rDTO.userId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("닉네임 조회 실패 - 존재하지 않는 userId")
    void getNicknameByUserId_userNotFound() throws Exception {
        // given
        UserInfoCommand pCommand = UserInfoCommand.builder()
                .userId(999L)
                .build();

        given(userInfoMapper.getNicknameByUserId(any()))
                .willReturn(null);

        // when & then
        assertThatThrownBy(() -> userInfoService.getNicknameByUserId(pCommand))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
                });
    }
}