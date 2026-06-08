package com.algotalk.userservice.service;

import com.algotalk.userservice.dto.request.SocialLinkRequestDTO;

public interface ISocialLinkService {
    // 소셜 계정 연동 토큰 발급
    String issueLinkToken(SocialLinkRequestDTO pDTO) throws Exception;
    // 소셜 계정 연동
    void linkSocialAccount(SocialLinkRequestDTO pDTO) throws Exception;
}
