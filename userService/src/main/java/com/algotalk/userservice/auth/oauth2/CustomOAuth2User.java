package com.algotalk.userservice.auth.oauth2;

import com.algotalk.userservice.auth.oauth2.info.OAuth2UserInfo;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CustomOAuth2User extends DefaultOAuth2User {

    private final OAuth2UserInfo oAuth2UserInfo; // OAuth2UserInfo 인터페이스를 구현한 클래스의 인스턴스를 저장
    private final Long userId; // 사용자 ID를 저장(신규 사용자면 null일 수 있음)
    private final boolean isNewUser; // 신규 사용자인지 여부를 저장

    public CustomOAuth2User(Collection<? extends GrantedAuthority> authorities,
                            Map<String, Object> attributes,
                            String nameAttributeKey,
                            OAuth2UserInfo oAuth2UserInfo,
                            Long userId,
                            boolean isNewUser) {
        super(authorities, attributes, nameAttributeKey);
        this.oAuth2UserInfo = oAuth2UserInfo;
        this.userId = userId;
        this.isNewUser = isNewUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oAuth2UserInfo.getAttributes();
    }

    @Override
    public String getName() {
        return oAuth2UserInfo.getName();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
