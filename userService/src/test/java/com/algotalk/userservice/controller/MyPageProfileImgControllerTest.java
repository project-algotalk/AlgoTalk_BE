package com.algotalk.userservice.controller;

import com.algotalk.common.response.ApiResponse;
import com.algotalk.userservice.client.CommunityFeignClient;
import com.algotalk.userservice.service.IEmailService;
import com.algotalk.userservice.service.IMypageService;
import com.algotalk.userservice.service.ISocialLinkService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MyPageProfileImgControllerTest {

    @Test
    @DisplayName("인증 사용자 ID와 multipart 파일로 프로필 이미지 변경 서비스를 호출한다")
    void updateProfileImg_success() throws Exception {
        IMypageService mypageService = mock(IMypageService.class);
        MyPageController controller = new MyPageController(
                mypageService,
                mock(IEmailService.class),
                mock(ISocialLinkService.class),
                mock(CommunityFeignClient.class));
        Jwt jwt = mock(Jwt.class);
        given(jwt.getSubject()).willReturn("7");
        MockMultipartFile file = new MockMultipartFile(
                "file", "profile.png", "image/png", "image".getBytes());
        String profileImgUrl = "https://bucket.s3.region.amazonaws.com/profile/7/new.png";
        given(mypageService.updateProfileImg(7L, file)).willReturn(profileImgUrl);

        ResponseEntity<ApiResponse<String>> response = controller.updateProfileImg(jwt, file);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isEqualTo(profileImgUrl);
        verify(mypageService).updateProfileImg(7L, file);
    }
}