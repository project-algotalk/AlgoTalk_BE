package com.algotalk.userservice.service.impl;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.repository.IUserUpdateMapper;
import com.algotalk.userservice.service.IEmailService;
import com.algotalk.userservice.service.IS3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.algotalk.userservice.exception.UserErrorCode.PROFILE_IMG_UPDATE_FAIL;
import static com.algotalk.userservice.exception.UserErrorCode.USER_NOT_FOUND;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MypageProfileImgServiceTest {

    @Mock
    private IUserUpdateMapper userUpdateMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private IEmailService emailService;
    @Mock
    private IS3Service s3Service;

    private MypageService mypageService;

    @BeforeEach
    void setUp() {
        mypageService = new MypageService(userUpdateMapper, passwordEncoder, emailService, s3Service);
    }

    @Test
    @DisplayName("프로필 이미지 변경 성공 시 새 URL을 저장하고 기존 이미지를 삭제한다")
    void updateProfileImg_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", "image".getBytes());
        String oldUrl = "https://bucket.s3.region.amazonaws.com/profile/1/old.png";
        String newUrl = "https://bucket.s3.region.amazonaws.com/profile/1/new.png";

        given(userUpdateMapper.getMyPageSummaryByUserId(1L))
                .willReturn(UserInfoCommand.builder()
                        .userId(1L)
                        .profileImgUrl(oldUrl)
                        .build());
        given(s3Service.uploadProfileImg(1L, file))
                .willReturn(newUrl);
        given(s3Service.getProfileImgUrl(newUrl))
                .willReturn(newUrl);
        given(userUpdateMapper.updateProfileImg(argThat(command ->
                command.getUserId().equals(1L)
                        && command.getProfileImgUrl().equals(newUrl))))
                .willReturn(1);

        String result = mypageService.updateProfileImg(1L, file);

        assertThat(result).isEqualTo(newUrl);
        verify(s3Service).getProfileImgUrl(newUrl);
        verify(s3Service).deleteProfileImg(oldUrl);
    }

    @Test
    @DisplayName("사용자가 없으면 S3에 업로드하지 않는다")
    void updateProfileImg_userNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", "image".getBytes());
        given(userUpdateMapper.getMyPageSummaryByUserId(1L)).willReturn(null);

        assertThatThrownBy(() -> mypageService.updateProfileImg(1L, file))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(USER_NOT_FOUND);
        verify(s3Service, never()).uploadProfileImg(1L, file);
    }

    @Test
    @DisplayName("DB 변경 실패 시 새로 업로드한 이미지를 삭제한다")
    void updateProfileImg_databaseUpdateFailureCompensatesUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", "image".getBytes());
        String newUrl = "https://bucket.s3.region.amazonaws.com/profile/1/new.png";
        given(userUpdateMapper.getMyPageSummaryByUserId(1L))
                .willReturn(UserInfoCommand.builder().userId(1L).build());
        given(s3Service.uploadProfileImg(1L, file)).willReturn(newUrl);
        given(userUpdateMapper.updateProfileImg(argThat(command -> command.getUserId().equals(1L))))
                .willReturn(0);

        assertThatThrownBy(() -> mypageService.updateProfileImg(1L, file))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(PROFILE_IMG_UPDATE_FAIL);
        verify(s3Service).deleteProfileImg(newUrl);
    }
}