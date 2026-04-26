package com.algotalk.userservice.auth.oauth2.info;

import com.algotalk.userservice.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.algotalk.userservice.domain.enums.LoginType.KAKAO;

@Slf4j
@RequiredArgsConstructor
public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override
    public String getProviderId() {
        String id = attributes.get("id").toString();
        return CmmUtil.nvl(id);
    }

    @Override
    public String getProvider() {
        return KAKAO.getProvider();
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");
        return CmmUtil.nvl((String) kakaoAccount.get("email"));
    }

    @Override
    public String getDisplayName() {
        Map<String, Object> kakaoAccount =
                (Map<String, Object>) attributes.get("kakao_account");

        if (kakaoAccount == null) return null;

        Map<String, Object> profile =
                (Map<String, Object>) kakaoAccount.get("profile");

        if (profile == null) return null;

        return CmmUtil.nvl((String) profile.get("nickname"));
    }
}
