package com.algotalk.userservice.auth.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

@Slf4j
@Configuration
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("{}.onAuthenticationSuccess() Start!", this.getClass().getSimpleName());

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        log.info("OAuth2 로그인 성공: provider: {}, providerId: {}, email: {}, name: {}",
                oAuth2User.getOAuth2UserInfo().getProvider(),
                oAuth2User.getOAuth2UserInfo().getProviderId(),
                oAuth2User.getOAuth2UserInfo().getEmail(),
                oAuth2User.getName());

        log.info("신규 회원 확인: {}", oAuth2User.isNewUser());

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write("OAuth2 로그인 성공!");

        log.info("{}.onAuthenticationSuccess() End!", this.getClass().getSimpleName());
    }
}
