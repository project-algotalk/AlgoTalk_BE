package com.algotalk.apigateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterSessionTest {

    @Mock
    private ReactiveJwtDecoder jwtDecoder;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtDecoder, new ObjectMapper().findAndRegisterModules(), redisTemplate);
        ReflectionTestUtils.setField(filter, "accessCookieName", "AccessToken");
        ReflectionTestUtils.setField(filter, "refreshCookieName", "RefreshToken");
        ReflectionTestUtils.setField(filter, "cookieSecure", false);
        ReflectionTestUtils.setField(filter, "sameSite", "Lax");
    }

    @Test
    @DisplayName("Redis 세션이 존재하면 인증 정보를 전달한다")
    void activeSessionPassesAuthentication() {
        Jwt jwt = accessToken("1", "session-a");
        given(jwtDecoder.decode("valid-token")).willReturn(Mono.just(jwt));
        given(redisTemplate.hasKey("refresh:1:session-a")).willReturn(Mono.just(true));

        MockServerWebExchange exchange = exchangeWithToken();
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        GatewayFilterChain chain = forwardedExchange -> {
            forwarded.set(forwardedExchange);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(forwarded.get()).isNotNull();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("1");
        assertThat(forwarded.get().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("USER");
        verify(redisTemplate).hasKey("refresh:1:session-a");
    }

    @Test
    @DisplayName("Redis 세션이 삭제되면 즉시 401을 반환하고 인증 쿠키를 만료한다")
    void revokedSessionReturnsUnauthorizedAndClearsCookies() {
        Jwt jwt = accessToken("1", "session-a");
        given(jwtDecoder.decode("valid-token")).willReturn(Mono.just(jwt));
        given(redisTemplate.hasKey("refresh:1:session-a")).willReturn(Mono.just(false));

        MockServerWebExchange exchange = exchangeWithToken();
        GatewayFilterChain chain = ignored -> Mono.error(new AssertionError("폐기된 세션은 전달되면 안 됩니다."));

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        List<String> setCookies = exchange.getResponse().getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookies).isNotNull();
        assertThat(setCookies).anyMatch(cookie -> cookie.startsWith("AccessToken=") && cookie.contains("Max-Age=0"));
        assertThat(setCookies).anyMatch(cookie -> cookie.startsWith("RefreshToken=") && cookie.contains("Max-Age=0"));
    }

    @Test
    @DisplayName("sessionId가 없는 이전 Access Token은 거부하고 Redis를 조회하지 않는다")
    void accessTokenWithoutSessionIdIsRejected() {
        Jwt jwt = Jwt.withTokenValue("legacy-token")
                .header("alg", "HS256")
                .subject("1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("roles", List.of("USER"))
                .build();
        given(jwtDecoder.decode("valid-token")).willReturn(Mono.just(jwt));

        MockServerWebExchange exchange = exchangeWithToken();

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.empty())).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(redisTemplate);
    }

    private MockServerWebExchange exchangeWithToken() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/api/user/v1/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
                .build());
    }

    private Jwt accessToken(String userId, String sessionId) {
        return Jwt.withTokenValue("valid-token")
                .header("alg", "HS256")
                .subject(userId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("roles", List.of("USER"))
                .claim("sessionId", sessionId)
                .build();
    }
}
