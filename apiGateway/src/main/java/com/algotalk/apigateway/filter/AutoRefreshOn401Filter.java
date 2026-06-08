package com.algotalk.apigateway.filter;

import com.algotalk.apigateway.auth.AuthFailureReason;
import com.algotalk.apigateway.auth.refresh.RefreshCoordinator;
import com.algotalk.apigateway.auth.refresh.RefreshOutcome;
import com.algotalk.apigateway.util.CmmUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.*;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * AT 만료 시 RT로 자동 재발급 후 원래 요청을 재시도하는 필터
 *
 * 동작 방식:
 * [401 감지 재시도] 다운스트림에서 401이 응답으로 내려오면 -> RT로 재발급 후 원래 요청 재시도 (1회)
 *
 * 실행 순서 (request 처리):
 * CookieToAuthHeaderFilter (HIGHEST_PRECEDENCE)     <- AT 쿠키 -> Authorization 헤더 변환
 * AutoRefreshOn401Filter   (HIGHEST_PRECEDENCE + 1) <- response 데코레이터 등록
 * JwtAuthenticationFilter  (HIGHEST_PRECEDENCE + 2) <- JWT 검증
 *
 * 실행 순서 (response 처리 - 역순):
 * JwtAuthenticationFilter  <- 401 writeWith() 호출
 * AutoRefreshOn401Filter   <- 401 가로챔 -> RT 재발급 -> 원래 요청 재시도
 * CookieToAuthHeaderFilter <- 통과
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class AutoRefreshOn401Filter implements GlobalFilter, Ordered {

    @Value("${jwt.token.access.name}")
    private String accessCookieName;

    @Value("${jwt.token.refresh.name}")
    private String refreshCookieName;

    @Value("${api.server.user.refresh-endpoint:/user/v1/token/reissue}")
    private String refreshPath;

    @Value("${api.server.user.protocol:http}://${api.server.user.host:localhost}:${api.server.user.port:10000}${api.server.user.refresh-endpoint:/user/v1/token/reissue}")
    private String refreshUrl;

    // User Service의 토큰 재발급 API를 호출
    private final WebClient webClient;

    // 같은 RT로 발생한 동시 재발급 요청을 하나로 합침
    private final RefreshCoordinator refreshCoordinator;

    // 동일한 원 요청이 재발급 실패 후 무한히 다시 시도하지 않도록 표시
    static final String ATTR_RETRIED = "X-RT-RETRIED";

    // JwtAuthenticationFilter가 기록한 인증 실패 원인을 읽기 위한 공통 attribute key
    static final String ATTR_AUTH_FAILURE_REASON = "AUTH_FAILURE_REASON";

    // 토큰 재발급/인증 관련 경로 - AutoRefresh 대상에서 제외
    private static final Set<String> SKIP_PATHS = java.util.Set.of(
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

    // GlobalFilter 진입점
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        HttpMethod method = exchange.getRequest().getMethod();

        // 스킵 대상 경로는 그대로 통과
        if (shouldSkip(path, method)) {
            return chain.filter(exchange);
        }

        // 401 감지 후 재시도 데코레이터 등록
        return on401RetryOnce(exchange, chain);
    }

    @Override
    public int getOrder() {
        // CookieToAuthHeaderFilter(HIGHEST_PRECEDENCE) 다음,
        // JwtAuthenticationFilter(HIGHEST_PRECEDENCE + 2) 이전에 실행
        // response 처리는 역순이므로 JwtFilter가 401을 내보내면 이 필터가 가로챔
        return Ordered.HIGHEST_PRECEDENCE + 1; // -2147483647
    }


    /**
     * 401 감지 후 재시도 로직
     *
     * 응답이 401이면 RT로 재발급 후 동일 요청을 1회 재시도
     * POST/PUT/PATCH의 경우 body를 캐싱하여 재시도 시 재사용
     */
    private Mono<Void> on401RetryOnce(ServerWebExchange exchange, GatewayFilterChain chain) {
        return cacheBodyIfNeeded(exchange).flatMap(ex -> {
            ServerHttpResponse original = ex.getResponse();

            // 응답을 가로채는 데코레이터
            ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(original) {
                @Override
                public @NonNull Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                    HttpStatusCode status = getStatusCode();
                    if (status == null) return super.writeWith(body);

                    // refresh 경로 자체는 재시도 대상에서 제외 (무한 루프 방지)
                    String path = ex.getRequest().getPath().value();
                    if (isSelfRefreshCall(path)) return super.writeWith(body);

                    // HTTP 상태, 재시도 여부, RT 존재 여부, 실제 JWT 실패 원인을 모두 확인하여 재발급 시도 여부 결정
                    boolean unauthorized = (status.value() == HttpStatus.UNAUTHORIZED.value()); // 401 응답인지 확인
                    boolean notRetried = !ex.getAttributeOrDefault(ATTR_RETRIED, Boolean.FALSE); // 이미 재시도한 요청인지 확인 (무한 루프 방지)
                    HttpCookie refreshCookie = ex.getRequest().getCookies().getFirst(refreshCookieName); // RT 쿠키 존재 여부 확인
                    boolean accessTokenExpired = ex.getAttribute(ATTR_AUTH_FAILURE_REASON)
                            == AuthFailureReason.ACCESS_TOKEN_EXPIRED; // JwtAuthenticationFilter가 기록한 실패 원인이 AT 만료인지 확인

                    // 서명 오류 같은 일반 401은 RT로 해결할 수 없으므로 AT 만료일 때만 재발급
                    if (!(unauthorized && accessTokenExpired && notRetried && refreshCookie != null)) {
                        return super.writeWith(body);
                    }

                    // 재시도 플래그 설정 (무한 루프 방지)
                    ex.getAttributes().put(ATTR_RETRIED, true);
                    log.debug("[AutoRefresh] 401 감지 -> RT로 재발급 시도: path={}", path);

                    // leader 요청만 실제 재발급 API를 호출하고 follower 요청은 같은 결과를 공유
                    return refreshCoordinator.refresh(refreshCookie.getValue(), () -> callRefresh(ex))
                            .flatMap(outcome -> {
                                if (outcome == null || CmmUtil.nvl(outcome.accessToken()).isBlank()) {
                                    // 재발급 실패 -> 원래 401 응답 그대로 반환
                                    log.debug("[AutoRefresh] 재발급 실패 -> 401 그대로 반환");
                                    return Mono.defer(() -> super.writeWith(body));
                                }
                                // 재발급 성공 -> 새 AT로 요청 재시도
                                log.debug("[AutoRefresh] 재발급 성공 -> 원래 요청 재시도");
                                // leader/follower 모두 같은 새 AT/RT 쿠키를 브라우저 응답에 넣도록 Set-Cookie 헤더 적용
                                applySetCookies(original, outcome.setCookies());
                                // 브라우저가 새 쿠키를 저장하기 전이므로 내부 재시도에는 새 AT를 직접 주입
                                ServerWebExchange retryEx = mutateWithNewAT(ex, outcome.accessToken());
                                return chain.filter(retryEx);
                            })
                            .switchIfEmpty(Mono.defer(() -> super.writeWith(body)));
                }
            };

            return chain.filter(ex.mutate().response(decorated).build());
        });
    }

    /**
     * 토큰 재발급 호출
     *
     * userService의 토큰 재발급 엔드포인트를 직접 호출한
     * RT는 쿠키 헤더에 포함되어 전달
     */
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
                    log.debug("[AutoRefresh] 재발급 요청 실패: {}", e.toString());
                    return Mono.empty();
                })
                .flatMap(res -> {
                    if (!res.getStatusCode().is2xxSuccessful()) return Mono.empty();
                    List<String> setCookies = res.getHeaders().get(HttpHeaders.SET_COOKIE);
                    String at = extractAt(setCookies);
                    if (at == null || at.isBlank()) {
                        at = extractAtFromAuthHeader(res.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
                    }
                    return Mono.justOrEmpty(new RefreshOutcome(at, setCookies));
                });
    }

    /**
     * 요청 body 캐싱 (POST/PUT/PATCH 재시도 시 body 재사용)
     *
     * POST/PUT/PATCH 요청의 body를 캐싱하여 재시도 시 재사용 가능하게 함
     * GET/DELETE 등은 body가 없으므로 캐싱하지 않음
     */
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
                    BodyInserter<byte[], org.springframework.http.ReactiveHttpOutputMessage> inserter =
                            BodyInserters.fromValue(bytes);

                    HttpHeaders headers = new HttpHeaders();
                    headers.putAll(exchange.getRequest().getHeaders());
                    headers.remove(HttpHeaders.CONTENT_LENGTH);
                    headers.remove(HttpHeaders.TRANSFER_ENCODING);

                    var cached = new CachedBodyOutputMessage(exchange, headers);
                    return inserter.insert(cached, new BodyInserterContext())
                            .then(Mono.defer(() -> {
                                ServerHttpRequestDecorator decorator =
                                        new ServerHttpRequestDecorator(exchange.getRequest()) {
                                            @Override
                                            public @NonNull HttpHeaders getHeaders() { return headers; }

                                            @Override
                                            public @NonNull Flux<DataBuffer> getBody() { return cached.getBody(); }
                                        };
                                return Mono.just(exchange.mutate().request(decorator).build());
                            }));
                });
    }

    // 헬퍼 메서드
    // refresh 엔드포인트 자체 호출인지 확인 (무한 루프 방지)
    private boolean isSelfRefreshCall(String path) {
        return CmmUtil.nvl(path).startsWith(CmmUtil.nvl(refreshPath, "/user/v1/token/reissue"));
    }

    //  AutoRefresh 대상에서 제외할 경로인지 확인
    private boolean shouldSkip(String path, HttpMethod method) {
        if (method == HttpMethod.OPTIONS) return true;
        return SKIP_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    // 응답의 Set-Cookie 헤더를 현재 응답에 추가
    private void applySetCookies(ServerHttpResponse resp, List<String> setCookies) {
        if (setCookies == null || setCookies.isEmpty()) return;
        setCookies.forEach(sc -> resp.getHeaders().add(HttpHeaders.SET_COOKIE, sc));
    }

    // 새 AT로 요청의 Authorization 헤더와 AT 쿠키를 교체
    private ServerWebExchange mutateWithNewAT(ServerWebExchange exchange, String at) {
        String newAt = CmmUtil.nvl(at);
        ServerHttpRequest.Builder rb = exchange.getRequest().mutate()
                .headers(h -> h.set(HttpHeaders.AUTHORIZATION, "Bearer " + newAt));

        // Cookie 헤더의 AT 쿠키도 새 값으로 교체
        String cookieHeader = CmmUtil.nvl(exchange.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE));
        String atPair = accessCookieName + "=" + newAt;

        if (cookieHeader.isBlank()) {
            rb.headers(h -> h.set(HttpHeaders.COOKIE, atPair));
        } else {
            String[] parts = cookieHeader.split(";\\s*");
            boolean replaced = false;
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].startsWith(accessCookieName + "=")) {
                    parts[i] = atPair;
                    replaced = true;
                    break;
                }
            }
            String merged = replaced ? String.join("; ", parts) : cookieHeader + "; " + atPair;
            rb.headers(h -> h.set(HttpHeaders.COOKIE, merged));
        }
        return exchange.mutate().request(rb.build()).build();
    }

    // Set-Cookie 헤더 목록에서 AT 값 추출
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

    // Authorization 헤더에서 Bearer 토큰 값만 추출
    private String extractAtFromAuthHeader(String authorization) {
        String auth = CmmUtil.nvl(authorization).strip();
        if (auth.isBlank()) return null;
        if (auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = auth.substring(7).strip();
            return token.isBlank() ? null : token;
        }
        return auth;
    }
}