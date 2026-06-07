package com.algotalk.apigateway.filter;

import com.algotalk.apigateway.auth.AuthFailureReason;
import com.algotalk.apigateway.exception.GatewayErrorCode;
import com.algotalk.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.algotalk.apigateway.exception.GatewayErrorCode.*;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final ReactiveJwtDecoder jwtDecoder;
    private final ObjectMapper objectMapper;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private static final List<String> PERMIT_ALL = List.of(
            "/api/user/v1/reg/**",
            "/api/user/v1/signup",
            "/api/user/v1/signup/social",
            "/api/cs-categories/v1/**", // 내부 서비스 간 통신
            "/api/user/v1/info/**",     // 내부 서비스 간 통신
            "/api/user/v1/login",
            "/api/user/v1/token/reissue",
            "/api/oauth2/**",
            "/api/login/oauth2/**",
            "/api/user/v1/find/**",
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // 1. 인증 필요 없는 경로 바로 다음 필터로 넘김
        // OPTIONS 요청은 인증 없이 허용 (CORS preflight 요청)
        if(exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            log.debug("OPTIONS 요청은 인증 없이 허용");
            return chain.filter(exchange); // 인증 안하고 다음 필터로 넘김 (CORS preflight 요청 허용)
        }

        // permitAll 경로는 토큰 검증 없이 바로 다음 필터로 넘김
        if(isPermitAll(path)) {
            log.debug("인증 제외 경로 :{}", path);

            // 스푸핑 방지 및 만료된 토큰 간섭 방지 위해 인증 제외 경로인 경우 관련 헤더 제거
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove(AUTHORIZATION);
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Role");
                    })
                    .build();

            return chain.filter(exchange.mutate().request(request).build());
        }

        // 그 외 경로는 JWT 검증
        // 2. Authorization 헤더에서 Bearer 토큰 추출
        String authHeader = exchange.getRequest()
                        .getHeaders().getFirst(AUTHORIZATION);

        if(authHeader == null) {
            log.warn("Authorization 헤더가 없음");
            return onError(exchange, TOKEN_NOT_FOUND);
        }

        if(!authHeader.startsWith("Bearer ")) {
            log.warn("Authorization 헤더가 Bearer 토큰이 아님");
            return onError(exchange, TOKEN_INVALID);
        }

        // 3. JwtDecoder로 토큰 검증
        String token = authHeader.substring(7).trim(); // "Bearer " 제거
        if(token.isEmpty()) {
            log.warn("Bearer 토큰이 비어 있음");
            return onError(exchange, TOKEN_INVALID);
        }

        // 검증 성공 시 SecurityContext에 인증 정보 저장 (필요 시)
        // 4. JwtDecoder로 토큰 검증
        return jwtDecoder.decode(token)
                .flatMap(jwt -> {
                    // 5. Jwt에서 필요한 정보 추출
                    String userId = jwt.getSubject();
                    List<String> roles = jwt.getClaimAsStringList("roles");

                    String role;
                    if(roles != null && !roles.isEmpty()) {
                        role = String.join(",", roles);
                    } else {
                        role = "";
                    }

                    log.debug("JWT 검증 성공 - userId: {}, role: {}", userId, role);

                    // 5. Header에 인증정보 추가
                    ServerHttpRequest mutateRequest = exchange.getRequest().mutate()
                            .headers(headers -> {
                                headers.remove("X-User-Id");
                                headers.remove("X-User-Role");
                                headers.set("X-User-Id", userId);
                                headers.set("X-User-Role", role);
                            })
                            .build();

                    return chain.filter(exchange.mutate().request(mutateRequest).build());
                })
                .onErrorResume(JwtException.class, e -> {
                    // 만료와 위조/형식 오류를 구분해야 불필요한 RT 회전을 막을 수 있다.
                    if (isExpired(e)) {
                        log.debug("JWT 만료: {}", e.getMessage());
                        exchange.getAttributes().put(
                                AutoRefreshOn401Filter.ATTR_AUTH_FAILURE_REASON,
                                AuthFailureReason.ACCESS_TOKEN_EXPIRED
                        );
                        return onError(exchange, TOKEN_EXPIRED);
                    }
                    log.warn("JWT 검증 실패: {}", e.getMessage());
                    exchange.getAttributes().put(
                            AutoRefreshOn401Filter.ATTR_AUTH_FAILURE_REASON,
                            AuthFailureReason.ACCESS_TOKEN_INVALID
                    );
                    return onError(exchange, TOKEN_INVALID);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2; // -2147483646
    }

    // permitAll 확인
    private boolean isPermitAll(String path) {
        return PERMIT_ALL.stream()
                .anyMatch(pattern ->
                        pathMatcher.match(pattern, path));
    }

    /**
     * Spring Security의 JWT 검증 오류 중 exp 만료 오류인지 확인한다.
     * 단순 파싱/서명 오류는 false를 반환하여 자동 재발급 대상에서 제외한다.
     */
    private boolean isExpired(JwtException exception) {
        if (!(exception instanceof JwtValidationException validationException)) {
            return false;
        }

        // JwtValidationException에는 여러 검증 오류가 들어올 수 있으므로 만료 설명이 하나라도 있는지 확인한다.
        return validationException.getErrors().stream()
                .map(error -> error.getDescription().toLowerCase(java.util.Locale.ROOT))
                .anyMatch(description -> description.contains("expired"));
    }

    // 인증 실패하는 경우 401 에러
    private Mono<Void> onError(ServerWebExchange exchange, GatewayErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(errorCode.getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        try {
            ErrorResponse errorResponse = ErrorResponse.of(errorCode);
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch(Exception e) {
            log.error("Error writing error response: {}", e.getMessage(), e);
            return response.setComplete(); // JSON 변환 실패 시 빈 응답으로 종료
        }
    }
}
