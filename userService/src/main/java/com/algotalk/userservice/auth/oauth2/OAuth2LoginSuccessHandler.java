package com.algotalk.userservice.auth.oauth2;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.userservice.dto.auth.RefreshTokenIssue;
import com.algotalk.userservice.dto.command.UserInfoCommand;
import com.algotalk.userservice.dto.request.SocialLinkRequestDTO;
import com.algotalk.userservice.service.IJwtTokenService;
import com.algotalk.userservice.service.IRefreshTokenService;
import com.algotalk.userservice.service.ISocialLinkService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.algotalk.userservice.exception.UserErrorCode.OAUTH2_LOGIN_FAILED;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final IJwtTokenService jwtTokenService;
    private final IRefreshTokenService refreshTokenService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ISocialLinkService socialLinkService;

    private static final String TEMP_TOKEN_PREFIX = "oauth2:temp:";
    private static final String LINK_TOKEN_PREFIX = "oauth2:link:";
    private static final String NONCE_PREFIX = "oauth2:nonce:";
    private static final String STATE_LINK_SEPARATOR = "::LINK::";

    @Value("${jwt.access.token.expiration}")
    private Long accessTokenExpiration; // ms

    @Value("${cookie.access.name}")
    private String accessTokenName;

    @Value("${cookie.refresh.name}")
    private String refreshTokenName;

    private static final long TEMP_TOKEN_TTL = 5 * 60L; // 5분

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.same-site}")
    private String sameSite;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String SIGNUP_PATH = "/oauth2/signup";
    private static final String CALLBACK_PATH = "/oauth2/callback";
    private static final String FAILURE_PATH = "/oauth2/failure";
    private static final String LINK_FAILURE_PATH = "/oauth2/link/failure";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("{}.onAuthenticationSuccess() Start!", this.getClass().getSimpleName());

        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        log.info("OAuth2 로그인 성공: provider: {}, 신규회원여부: {}",
                oAuth2User.getOAuth2UserInfo().getProvider(),
                oAuth2User.isNewUser());

        String linkToken = extractLinkToken(request);
        log.info("신규 회원 확인: {}, linkToken 존재 여부: {}", oAuth2User.isNewUser(), linkToken != null);

        if (linkToken != null) {
            handleLinkAccount(response, oAuth2User, linkToken);
        } else if (oAuth2User.isNewUser()) {
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
        tempData.put("name",       oAuth2User.getOAuth2UserInfo().getDisplayName());

        redisTemplate.opsForHash().putAll(TEMP_TOKEN_PREFIX + tempToken, tempData);
        redisTemplate.expire(TEMP_TOKEN_PREFIX + tempToken, TEMP_TOKEN_TTL, TimeUnit.SECONDS);

        log.info("임시 토큰 발급 완료");

        String encodedToken = URLEncoder.encode(tempToken, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + SIGNUP_PATH + "?tempToken=" + encodedToken);

        log.info("{}.handleNewUser() End!", this.getClass().getSimpleName());
    }

    // 기존 회원: JWT 발급 후 메인 페이지로 리다이렉트
    private void handleExistingUser(HttpServletResponse response, CustomOAuth2User oAuth2User) throws IOException {
        log.info("{}.handleExistingUser() Start!", this.getClass().getSimpleName());

        log.info("기존 소셜 회원 처리 시작");

        try {
            UserInfoCommand userInfo = UserInfoCommand.builder()
                    .userId(oAuth2User.getUserId())
                    .loginId("") // 로그인 ID는 소셜 로그인에서는 사용하지 않으므로 빈 문자열로 설정
                    .nickname(oAuth2User.getNickname()) // 기존 회원은 DB에 저장된 닉네임 사용
                    .role(oAuth2User.getRole()) // 기존 회원은 DB에 저장된 권한 사용
                    .build();

            String accessToken = jwtTokenService.generateAccessToken(userInfo);
            RefreshTokenIssue refreshTokenIssue =
                    jwtTokenService.issueRefreshToken(userInfo);

            refreshTokenService.saveRefreshToken(
                    oAuth2User.getUserId(),
                    refreshTokenIssue.sessionId(),
                    refreshTokenIssue.token(),
                    refreshTokenIssue.expiresAt()
            );

            setAccessTokenCookie(accessToken, response); // 리다이렉트하기 때문에 헤더로 전달 불가능
            setRefreshTokenCookie(refreshTokenIssue.token(), refreshTokenIssue.expiresAt(), response);

            log.info("JWT 발급 완료");

            response.sendRedirect(frontendUrl + CALLBACK_PATH);

        } catch (Exception e) {
            log.error("기존 소셜 회원 JWT 발급 중 오류", e);

            String errorCode = URLEncoder.encode(OAUTH2_LOGIN_FAILED.getCode(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendUrl + FAILURE_PATH + "?error=" + errorCode);
        }

        log.info("{}.handleExistingUser() End!", this.getClass().getSimpleName());
    }

    private void setAccessTokenHeader(String accessToken, HttpServletResponse response) {
        response.setHeader(AUTHORIZATION, "Bearer " + accessToken);
    }

    private void setAccessTokenCookie(String accessToken, HttpServletResponse response) {
        ResponseCookie atCookie = ResponseCookie.from(accessTokenName, accessToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(accessTokenExpiration / 1000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, atCookie.toString());
    }

    private void setRefreshTokenCookie(String refreshToken, Instant expiresAt, HttpServletResponse response) {
        ResponseCookie rtCookie = ResponseCookie.from(refreshTokenName, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/")
                .sameSite(sameSite)
                .maxAge(Math.max(0, Duration.between(Instant.now(), expiresAt).toSeconds()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, rtCookie.toString());
    }

    private String extractLinkToken(HttpServletRequest request) {
        String state = request.getParameter("state");
        if (state == null) {
            return null;
        }

        int idx = state.lastIndexOf(STATE_LINK_SEPARATOR);
        if (idx < 0) {
            return null;
        }

        String nonce = state.substring(idx + STATE_LINK_SEPARATOR.length());
        if (nonce.isBlank()) {
            return null;
        }

        String linkToken = (String) redisTemplate.opsForValue().get(NONCE_PREFIX + nonce);
        if (linkToken == null) {
            return null;
        }

        redisTemplate.delete(NONCE_PREFIX + nonce);
        return linkToken;
    }

    private void handleLinkAccount(HttpServletResponse response, CustomOAuth2User oAuth2User, String linkToken) throws IOException {
        try {
            String key = LINK_TOKEN_PREFIX + linkToken;
            Map<Object, Object> tokenData = redisTemplate.opsForHash().entries(key);
            if (tokenData.isEmpty()) {
                String error = URLEncoder.encode("LINK_TOKEN_EXPIRED", StandardCharsets.UTF_8);
                response.sendRedirect(frontendUrl + LINK_FAILURE_PATH + "?error=" + error);
                return;
            }

            redisTemplate.delete(key);

            Long userId = Long.valueOf(tokenData.get("userId").toString());
            String expectedProvider = tokenData.get("provider").toString();
            String actualProvider = oAuth2User.getOAuth2UserInfo().getProvider().toUpperCase();
            String providerId = oAuth2User.getOAuth2UserInfo().getProviderId();

            if (!expectedProvider.equals(actualProvider)) {
                String error = URLEncoder.encode("PROVIDER_MISMATCH", StandardCharsets.UTF_8);
                response.sendRedirect(frontendUrl + LINK_FAILURE_PATH + "?error=" + error);
                return;
            }

            socialLinkService.linkSocialAccount(SocialLinkRequestDTO.builder()
                    .userId(userId)
                    .provider(actualProvider)
                    .providerId(providerId)
                    .build());
            response.sendRedirect(frontendUrl + "/oauth2/link/success");
        } catch (BusinessException be) {
            // 중복 연결 등의 비즈니스 예외는 에러코드 그대로 전달
            String error = URLEncoder.encode(be.getErrorCode().getCode(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendUrl + LINK_FAILURE_PATH + "?error=" + error);
        } catch (Exception e) {
            log.error("소셜 계정 연결 중 오류", e);
            String errorCode = URLEncoder.encode(OAUTH2_LOGIN_FAILED.getCode(), StandardCharsets.UTF_8);
            response.sendRedirect(frontendUrl + LINK_FAILURE_PATH + "?error=" + errorCode);
        }
    }
}
