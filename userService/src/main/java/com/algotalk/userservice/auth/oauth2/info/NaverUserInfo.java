package com.algotalk.userservice.auth.oauth2.info;

import com.algotalk.userservice.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.algotalk.userservice.domain.enums.LoginType.NAVER;

@Slf4j
@RequiredArgsConstructor
public class NaverUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getProviderId() {
        // Naver는 response 객체 안에 id가 있음
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) return null;

        String id = response.get("id").toString();
        return CmmUtil.nvl(id);
    }

    @Override
    public String getProvider() {
        return NAVER.getProvider();
    }

    @Override
    public String getEmail() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) return null;
        return (String) response.get("email");
    }

    @Override
    public String getDisplayName() {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) return null;
        return (String) response.get("name");
    }
}
