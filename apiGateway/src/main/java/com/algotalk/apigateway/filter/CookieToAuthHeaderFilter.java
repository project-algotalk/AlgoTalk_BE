package com.algotalk.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 쿠키에 저장된 AccessToken을 Authorization 헤더로 변환하는 필터
 *
 * 브라우저는 쿠키를 자동으로 포함하지만 Authorization 헤더는 수동으로 세팅해야 하므로,
 * AT 쿠키가 존재하고 Authorization 헤더가 없을 때 헤더로 변환하여 하위 필터에 전달한다.
 *
 * 실행 순서:
 * CookieToAuthHeaderFilter (HIGHEST_PRECEDENCE)     ← AT 쿠키 → Authorization 헤더 변환
 * AutoRefreshOn401Filter   (HIGHEST_PRECEDENCE + 1) ← 401 감지 → 재발급
 * JwtAuthenticationFilter  (HIGHEST_PRECEDENCE + 2) ← JWT 검증
 */
@Component
@Slf4j
public class CookieToAuthHeaderFilter implements GlobalFilter, Ordered {

    @Value("${jwt.token.access.name}")
    private String accessCookieName;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();

        // OPTIONS 요청은 패스
        if (req.getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // Authorization 헤더가 이미 있으면 그대로 통과
        String authorization = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization)) {
            return chain.filter(exchange);
        }

        // AT 쿠키가 있으면 Authorization 헤더로 변환
        HttpCookie cookie = req.getCookies().getFirst(accessCookieName);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            ServerHttpRequest mutated = req.mutate()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cookie.getValue())
                    .build();
            log.debug("[CookieToAuthHeader] AT 쿠키 → Authorization 헤더 변환: path={}", req.getPath());
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // AutoRefreshOn401Filter(HIGHEST_PRECEDENCE + 1), JwtAuthenticationFilter(HIGHEST_PRECEDENCE + 2) 보다 먼저 실행
        return Ordered.HIGHEST_PRECEDENCE; // -2147483648
    }
}