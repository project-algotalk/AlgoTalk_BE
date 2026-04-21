package com.algotalk.userservice.auth.oauth2;

import com.algotalk.userservice.auth.oauth2.info.GoogleUserInfo;
import com.algotalk.userservice.auth.oauth2.info.OAuth2UserInfo;
import com.algotalk.userservice.dto.command.SocialAccountCommand;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.repository.ISocialAccountMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.algotalk.userservice.exception.UserErrorCode.OAUTH2_LOGIN_FAILED;
import static com.algotalk.userservice.exception.UserErrorCode.OAUTH2_PROVIDER_NOT_SUPPORTED;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final ISocialAccountMapper socialAccountMapper;

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
                oAuth2UserInfo.getDisplayName()
        );

        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        // 4. provider + providerId로 DB 조회 -> 신규 회원이면 userId는 null, isNewUser는 true로 반환
        UserInfoCommand existingUser = null;
        boolean isNewUser = true; // 신규(true) or 기존(false)

        try {
            SocialAccountCommand pCommand = SocialAccountCommand.builder()
                    .provider(oAuth2UserInfo.getProvider())
                    .providerId(oAuth2UserInfo.getProviderId())
                    .build();

            existingUser = socialAccountMapper.findByProviderAndProviderId(pCommand);

            if(existingUser != null) {
                log.info("기존 회원 userId: {}", existingUser.getUserId());
                isNewUser = false; // 기존 회원이면 false로 설정
            }

        } catch (Exception e) {
            log.error("DB 조회 중 오류 발생: {}", e.getMessage());
            throw new OAuth2AuthenticationException(new OAuth2Error(
                    OAUTH2_LOGIN_FAILED.getCode(),
                    OAUTH2_LOGIN_FAILED.getMessage(),
                    null
            ));
        }

        String resolvedRole = existingUser != null ? existingUser.getRole() : "ROLE_USER";

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(resolvedRole));

        log.info("{}.loadUser()End!", this.getClass().getSimpleName());

        return new CustomOAuth2User(
            authorities,
            oAuth2User.getAttributes(),
            nameAttributeKey,
            oAuth2UserInfo,
            existingUser != null ? existingUser.getUserId() : null, // 기존 회원이면 userId, 신규 회원이면 null
            existingUser != null ? existingUser.getNickname() : oAuth2UserInfo.getDisplayName(), // 기존 회원이면 DB에서 닉네임, 신규 회원이면 OAuth2UserInfo의 name
            resolvedRole,
            isNewUser // 신규 회원 여부
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
