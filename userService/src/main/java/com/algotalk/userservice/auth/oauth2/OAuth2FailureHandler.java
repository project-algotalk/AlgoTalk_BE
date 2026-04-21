package com.algotalk.userservice.auth.oauth2;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static com.algotalk.userservice.exception.UserErrorCode.OAUTH2_LOGIN_FAILED;

@Slf4j
@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String FAILURE_PATH = "/oauth2/failure";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.info("{}.onAuthenticationFailure() Start!", this.getClass().getSimpleName());
        log.warn("OAuth2 로그인 실패", exception);

        // OAuth2AuthenticationException이면 구체적인 에러 코드 추출해서 전달 할 수 있도록 함
        String errorCode;
        if (exception instanceof OAuth2AuthenticationException oAuth2AE) {
            errorCode = oAuth2AE.getError().getErrorCode();
        } else {
            errorCode = OAUTH2_LOGIN_FAILED.getCode();
        }        response.sendRedirect(frontendUrl + FAILURE_PATH + "?error=" + errorCode);

        log.info("{}.onAuthenticationFailure() End!", this.getClass().getSimpleName());
    }
}
