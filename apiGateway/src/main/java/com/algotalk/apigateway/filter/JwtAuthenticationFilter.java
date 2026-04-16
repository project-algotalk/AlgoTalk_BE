package com.algotalk.apigateway.filter;

import com.algotalk.apigateway.exception.GatewayErrorCode;
import com.algotalk.common.response.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static com.algotalk.apigateway.exception.GatewayErrorCode.TOKEN_INVALID;
import static com.algotalk.apigateway.exception.GatewayErrorCode.TOKEN_NOT_FOUND;
import static org.apache.http.HttpHeaders.AUTHORIZATION;

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
            "/api/user/v1/login",
            "/api/user/v1/token/reissue",
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        // 1. permitAll 경로는 토큰 검증 없이 바로 다음 필터로 넘김
        if(isPermitAll(path)) {
            log.info("인증 제외 경로 :{}", path);
            return chain.filter(exchange);
        }

        // 그 외 경로는 JWT 검증
        // 2. Authorization 헤더에서 Bearer 토큰 추출
        String authHeader = exchange.getRequest()
                        .getHeaders().getFirst(AUTHORIZATION);

        if(authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Authorization 헤더가 없거나 Bearer 토큰이 아님");
            return onError(exchange, TOKEN_NOT_FOUND);
        }

        // 3. JwtDecoder로 토큰 검증
        String token = authHeader.substring(7); // "Bearer " 제거

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

                    log.info("JWT 검증 성공 - userId: {}, role: {}", userId, role);

                    // 5. Header에 인증정보 추가
                    ServerHttpRequest mutateRequest = exchange.getRequest().mutate()
                            .headers(headers -> {
                                    headers.add("X-User-Id", userId);
                                    headers.add("X-User-Role", role);
                            })
                            .build();

                    return chain.filter(exchange.mutate().request(mutateRequest).build());
                })
                .onErrorResume(Exception.class, e -> {
                    log.warn("JWT 검증 실패: {}", e.getMessage());
                    return onError(exchange, TOKEN_INVALID);
                });
    }

    @Override
    public int getOrder() {
        // 인증 필터가 가장 먼저 실행되어야 해서 낮은 순자로 반환
        return Ordered.HIGHEST_PRECEDENCE;
    }

    // permitAll 확인
    private boolean isPermitAll(String path) {
        return PERMIT_ALL.stream()
                .anyMatch(pattern ->
                        pathMatcher.match(pattern, path));
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
