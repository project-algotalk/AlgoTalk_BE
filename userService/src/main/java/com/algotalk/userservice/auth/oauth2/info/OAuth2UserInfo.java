package com.algotalk.userservice.auth.oauth2.info;

import java.util.Map;

public interface OAuth2UserInfo {
    public Map<String, Object> getAttributes();
    String getProviderId();
    String getProvider();
    String getEmail();
    String getName();
}
