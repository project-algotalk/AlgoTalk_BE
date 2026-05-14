package com.algotalk.apigateway.filter;

import com.algotalk.apigateway.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Component
@Order(-102)
public class AutoRefreshOn401Filter implements WebFilter {

    @Value("${jwt.token.access.name}")
    private String accessCookieName;

    @Value("${jwt.token.refresh.name}")
    private String refreshCookieName;

    @Value("${api.server.user.refresh-endpoint:/user/v1/token/reissue}")
    private String refreshPath;

    @Value("${api.server.user.protocol:http}://${api.server.user.host:localhost}:${api.server.user.port:10000}${api.server.user.refresh-endpoint:/user/v1/token/reissue}")
    private String refreshUrl;

    private final WebClient webClient;

    private static final String ATTR_RETRIED = "X-RT-RETRIED";

    private static final Set<String> SKIP_PATHS = Set.of(
            "/api/user/v1/login",
            "/api/user/v1/reg/**",
            "/api/user/v1/signup",
            "/api/user/v1/signup/social",
            "/api/user/v1/find/**",
            "/api/oauth2/**",
            "/api/login/oauth2/**",
            "/api/user/v1/token/reissue"
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private record RefreshOutcome(String at, List<String> setCookies) {}

    private boolean isSelfRefreshCall(String path) {
        return CmmUtil.nvl(path).startsWith(CmmUtil.nvl(refreshPath, "/user/v1/token/reissue"));
    }


    private boolean shouldSkip(String path, HttpMethod method) {
        if (method == HttpMethod.OPTIONS) return true;
        return SKIP_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private String extractAuthOrAtCookie(ServerWebExchange exchange) {
        String auth = CmmUtil.nvl(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        if (!auth.isBlank()) return auth;
        var atCookie = exchange.getRequest().getCookies().getFirst(accessCookieName);
        return (atCookie == null) ? "" : "Bearer " + CmmUtil.nvl(atCookie.getValue());
    }

    private boolean needPreRefresh(ServerWebExchange exchange) {
        boolean hasRt = exchange.getRequest().getCookies().containsKey(refreshCookieName);
        if (!hasRt) return false;
        return extractAuthOrAtCookie(exchange).isBlank();
    }

    private void applySetCookies(ServerHttpResponse resp, List<String> setCookies) {
        if (setCookies == null || setCookies.isEmpty()) return;
        setCookies.forEach(sc -> resp.getHeaders().add(HttpHeaders.SET_COOKIE, sc));
    }

    private ServerWebExchange mutateWithNewAT(ServerWebExchange exchange, String at) {
        String newAt = CmmUtil.nvl(at);
        ServerHttpRequest.Builder rb = exchange.getRequest().mutate()
                .headers(h -> h.set(HttpHeaders.AUTHORIZATION, "Bearer " + newAt));

        String cookieHeader = CmmUtil.nvl(exchange.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE));
        String atPair = accessCookieName + "=" + newAt;
        if (!cookieHeader.contains(accessCookieName + "=")) {
            String merged = cookieHeader.isBlank() ? atPair : cookieHeader + "; " + atPair;
            rb.headers(h -> h.set(HttpHeaders.COOKIE, merged));
        }
        return exchange.mutate().request(rb.build()).build();
    }

    private String extractAt(List<String> setCookies) {
        if (setCookies == null) return null;
        String prefix = accessCookieName + "=";
        for (String sc : setCookies) {
            int i = sc.indexOf(prefix);
            if (i >= 0) {
                String sub = sc.substring(i + prefix.length());
                int semi = sub.indexOf(';');
                return (semi >= 0) ? sub.substring(0, semi) : sub;
            }
        }
        return null;
    }

    private String extractAtFromAuthHeader(String authorization) {
        String auth = CmmUtil.nvl(authorization.strip());
        if (auth.isBlank()) return null;
        if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = auth.substring(7).strip();
            return token.isBlank() ? null : token;
        }
        return auth;
    }

    private Mono<RefreshOutcome> callRefresh(ServerWebExchange exchange) {
        String cookieHeader = CmmUtil.nvl(exchange.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE));
        String ua = CmmUtil.nvl(exchange.getRequest().getHeaders().getFirst(HttpHeaders.USER_AGENT));

        return webClient.post()
                .uri(refreshUrl)
                .headers(h -> {
                    if (!cookieHeader.isBlank()) h.set(HttpHeaders.COOKIE, cookieHeader);
                    if (!ua.isBlank()) h.set(HttpHeaders.USER_AGENT, ua);
                    h.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
                    h.setContentLength(0);
                })
                .retrieve()
                .toEntity(byte[].class)
                .timeout(Duration.ofSeconds(3))
                .onErrorResume(e -> {
                    log.debug("[AutoRefresh] refresh error: {}", e.toString());
                    return Mono.empty();
                })
                .flatMap(res -> {
                    if (!res.getStatusCode().is2xxSuccessful()) return Mono.empty();
                    List<String> setCookies = res.getHeaders().get(HttpHeaders.SET_COOKIE);
                    String at = extractAt(setCookies);
                    if (at == null || at.isBlank()) {
                        at = extractAtFromAuthHeader(res.getHeaders().getFirst(HttpHeaders.SET_COOKIE));
                    }
                    return Mono.justOrEmpty(new RefreshOutcome(at, setCookies));
                });
    }

    private Mono<ServerWebExchange> cacheBodyIfNeeded(ServerWebExchange exchange) {
        HttpMethod m = exchange.getRequest().getMethod();
        boolean needsBody = (m == HttpMethod.POST || m == HttpMethod.PUT || m == HttpMethod.PATCH);
        if (!needsBody) return Mono.just(exchange);

        String path = exchange.getRequest().getPath().value();
        if (isSelfRefreshCall(path)) return Mono.just(exchange);

        MediaType ct = exchange.getRequest().getHeaders().getContentType();
        if (ct == null || !MediaType.APPLICATION_JSON.isCompatibleWith(ct)) return Mono.just(exchange);

        return ServerRequest.create(exchange, HandlerStrategies.withDefaults().messageReaders())
                .bodyToMono(byte[].class)
                .defaultIfEmpty(new byte[0])
                .flatMap(bytes -> {
                    BodyInserter<byte[], org.springframework.http.ReactiveHttpOutputMessage> inserter = BodyInserters.fromValue(bytes);

                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(exchange.getRequest().getHeaders());
                    headers.remove(HttpHeaders.CONTENT_LENGTH);
                    headers.remove(HttpHeaders.TRANSFER_ENCODING);

                    var cached = new CachedBodyOutputMessage(exchange, headers);
                    return inserter.insert(cached, new BodyInserterContext())
                            .then(Mono.defer(() -> {
                                ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(exchange.getRequest()) {
                                    @Override
                                    public @NonNull HttpHeaders getHeaders() {
                                        return headers;
                                    }

                                    @Override
                                    public @NonNull Flux<DataBuffer> getBody() {
                                        return cached.getBody();
                                    }
                                };
                                return Mono.just(exchange.mutate().request(decorator).build());
                            }));
                });
    }

    private Mono<Void> on401RetryOnce(ServerWebExchange exchange, WebFilterChain chain) {
        return cacheBodyIfNeeded(exchange).flatMap(ex -> {
            ServerHttpResponse original = ex.getResponse();

            ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(original) {
                @Override
                public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                    HttpStatusCode status = getStatusCode();
                    if (status == null) return super.writeWith(body);

                    String path = ex.getRequest().getPath().value();
                    if (isSelfRefreshCall(path)) return super.writeWith(body);

                    boolean unauthorized = (status.value() == HttpStatus.UNAUTHORIZED.value());
                    boolean notRetried = ex.getAttributeOrDefault(ATTR_RETRIED, Boolean.FALSE) == Boolean.FALSE;
                    boolean hasRt = ex.getRequest().getCookies().containsKey(refreshCookieName);

                    if (!(unauthorized && notRetried && hasRt)) return super.writeWith(body);

                    ex.getAttributes().put(ATTR_RETRIED, true);

                    return callRefresh(ex)
                            .flatMap(outcome -> {
                                if (outcome == null || CmmUtil.nvl(outcome.at()).isBlank()) {
                                    return Mono.defer(() -> super.writeWith(body));
                                }
                                applySetCookies(original, outcome.setCookies());
                                ServerWebExchange retryEx = mutateWithNewAT(ex, outcome.at());
                                return chain.filter(retryEx);
                            })
                            .switchIfEmpty(Mono.defer(() -> super.writeWith(body)));
                }
            };

            return chain.filter(ex.mutate().response(decorated).build());
        });
    }

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();

        if (shouldSkip(path, method)) {
            return chain.filter(exchange);
        }

        if (!isSelfRefreshCall(path) && needPreRefresh(exchange)) {
            return callRefresh(exchange)
                    .flatMap(outcome -> {
                        if (outcome != null && !CmmUtil.nvl(outcome.at()).isBlank()) {
                            applySetCookies(exchange.getResponse(), outcome.setCookies());
                            ServerWebExchange resumed = mutateWithNewAT(exchange, outcome.at());
                            return chain.filter(resumed);
                        }
                        return on401RetryOnce(exchange, chain);
                    })
                    .switchIfEmpty(on401RetryOnce(exchange, chain));
        }

        return on401RetryOnce(exchange, chain);
    }
}