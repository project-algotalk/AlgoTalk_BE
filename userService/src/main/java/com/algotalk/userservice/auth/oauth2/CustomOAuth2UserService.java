package com.algotalk.userservice.auth.oauth2;

import com.algotalk.userservice.auth.oauth2.info.GoogleUserInfo;
import com.algotalk.userservice.auth.oauth2.info.OAuth2UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.algotalk.userservice.exception.UserErrorCode.OAUTH2_PROVIDER_NOT_SUPPORTED;

@Slf4j
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("{}.loadUser() Start!", this.getClass().getSimpleName());

        // 1. Google에서 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. provider 확인
        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        log.info("OAuth2 provider: {}", registrationId);

        // 3. provider에 따라서 userInfo 객체 생성
        OAuth2UserInfo oAuth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User);

        log.info("OAuth2 사용자 정보: provider: {}, providerId: {}, email: {}, name: {}",
                oAuth2UserInfo.getProvider(),
                oAuth2UserInfo.getProviderId(),
                oAuth2UserInfo.getEmail(),
                oAuth2UserInfo.getName());

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        log.info("{}.loadUser()End!", this.getClass().getSimpleName());

        // 4. DB 접근 업이 신규 회원으로 반환 진행
        return new CustomOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                oAuth2User.getAttributes(),
                nameAttributeKey,
                oAuth2UserInfo,
                null, // userId는 DB 접근 없이 신규 회원으로 반환하므로 null
                true  // isNewUser는 항상 true로 설정
        );
    }

    // 제공자에 따라서 userInfo 객체 생성하는 메서드
    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, OAuth2User oAuth2User) {
        if (registrationId.equalsIgnoreCase("google")) {
            return new GoogleUserInfo(oAuth2User.getAttributes());
        }

        log.warn("지원하지 않는 OAuth2 provider: {}", registrationId);
        throw new OAuth2AuthenticationException(new OAuth2Error(
                OAUTH2_PROVIDER_NOT_SUPPORTED.getCode(),
                OAUTH2_PROVIDER_NOT_SUPPORTED.getMessage(),
                null
        ));
    }
}
