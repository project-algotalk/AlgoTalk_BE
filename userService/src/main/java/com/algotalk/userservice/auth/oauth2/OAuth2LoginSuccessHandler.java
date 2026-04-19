package com.algotalk.userservice.auth.oauth2;

import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IRefreshTokenService;
import com.algotalk.userservice.service.impl.JwtTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final IJwtTokenService jwtTokenService;
    private final IRefreshTokenService refreshTokenService;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TEMP_TOKEN_PREFIX = "oauth2:temp:";

    @Value("${jwt.refresh.token.expiration}")
    private Long refreshTokenExpiration; // ms

    private static final long TEMP_TOKEN_TTL = 5 * 60L; // 5분

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String sameSite;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String SIGNUP_PATH = "/oauth2/signup";
    private static final String CALLBACK_PATH = "/oauth2/callback";

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

        if (oAuth2User.isNewUser()) {
            handleNewUser(response, oAuth2User);
        } else {
            handleExistingUser(response, oAuth2User);
        }

        log.info("{}.onAuthenticationSuccess() End!", this.getClass().getSimpleName());
    }

    // 신규 회원: Redis에 임시토큰 발급하고 회원가입 페이지로 리다이렉트
    private void handleNewUser(HttpServletResponse response, CustomOAuth2User oAuth2User) throws IOException {
        log.info("{}.handleNewUser() Start!", this.getClass().getSimpleName());

        String tempToken = UUID.randomUUID().toString();

        Map<String, String> tempData = new HashMap<>();
        tempData.put("provider",   oAuth2User.getOAuth2UserInfo().getProvider());
        tempData.put("providerId", oAuth2User.getOAuth2UserInfo().getProviderId());
        tempData.put("email",      oAuth2User.getOAuth2UserInfo().getEmail());
        tempData.put("name",       oAuth2User.getOAuth2UserInfo().getName());

        redisTemplate.opsForHash().putAll(TEMP_TOKEN_PREFIX + tempToken, tempData);
        redisTemplate.expire(TEMP_TOKEN_PREFIX + tempToken, TEMP_TOKEN_TTL, TimeUnit.SECONDS);

        log.info("임시 토큰 발급 완료: tempToken={}", tempToken);

        response.sendRedirect(frontendUrl + SIGNUP_PATH + "?tempToken=" + tempToken);

        log.info("{}.handleNewUser() End!", this.getClass().getSimpleName());
    }

    // 기존 회원: JWT 발급 후 메인 페이지로 리다이렉트
    private void handleExistingUser(HttpServletResponse response, CustomOAuth2User oAuth2User) throws IOException {
        log.info("{}.handleExistingUser() Start!", this.getClass().getSimpleName());

        log.info("기존 소셜 회원 처리 시작: userId={}", oAuth2User.getUserId());

        try {
            UserInfoCommand userInfo = UserInfoCommand.builder()
                    .userId(oAuth2User.getUserId())
                    .loginId("") // 로그인 ID는 소셜 로그인에서는 사용하지 않으므로 빈 문자열로 설정
                    .nickname(oAuth2User.getName()) // 닉네임은 OAuth2User의 name으로 설정
                    .role("ROLE_USER")
                    .build();

            String accessToken = jwtTokenService.generateAccessToken(userInfo);
            String refreshToken = jwtTokenService.generateRefreshToken(userInfo);

            // RT Redis 저장
            refreshTokenService.saveRefreshToken(oAuth2User.getUserId(), refreshToken);

            // RT Cookie 설정
            ResponseCookie rtCookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(cookieSecure) // yml 설정값 사용
                    .path("/")
                    .sameSite(sameSite) // yml 설정값 사용
                    .maxAge(refreshTokenExpiration / 1000)
                    .build();
            response.addHeader("Set-Cookie", rtCookie.toString());

            log.info("JWT 발급 완료: userId={}", oAuth2User.getUserId());

            // AT는 fragment로 전달 (URL query 노출 방지)
            response.sendRedirect(frontendUrl + CALLBACK_PATH + "#token=" + accessToken);

        } catch (Exception e) {
            log.error("기존 소셜 회원 JWT 발급 중 오류: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "로그인 처리 중 오류가 발생했습니다.");
        }

        log.info("{}.handleExistingUser() End!", this.getClass().getSimpleName());
    }
}
