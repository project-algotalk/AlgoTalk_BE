package com.algotalk.userservice.auth.oauth2.info;

import java.util.Map;

public interface OAuth2UserInfo {
    Map<String, Object> getAttributes();
    String getProviderId();
    String getProvider();
    String getEmail();
    String getDisplayName();
}
