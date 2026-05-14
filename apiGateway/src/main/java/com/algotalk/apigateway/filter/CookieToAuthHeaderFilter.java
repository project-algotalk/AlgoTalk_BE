package com.algotalk.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@Order(-101)
public class CookieToAuthHeaderFilter implements WebFilter {

    @Value("${jwt.token.access.name}")
    private String accessCookieName;

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String authorization = req.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (StringUtils.hasText(authorization)) {
            return chain.filter(exchange);
        }

        HttpCookie cookie = req.getCookies().getFirst(accessCookieName);
        if (cookie != null && StringUtils.hasText(cookie.getValue())) {
            ServerHttpRequest mutated = req.mutate()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + cookie.getValue())
                    .build();
            log.debug("CookieToAuthHeaderFilter applied for path={}", req.getPath());
            return chain.filter(exchange.mutate().request(mutated).build());
        }

        return chain.filter(exchange);
    }
}