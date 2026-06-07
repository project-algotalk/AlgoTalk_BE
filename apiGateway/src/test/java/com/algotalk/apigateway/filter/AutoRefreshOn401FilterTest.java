package com.algotalk.apigateway.filter;

import com.algotalk.apigateway.auth.refresh.RefreshCoordinator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import static org.junit.jupiter.api.Assertions.*;

class AutoRefreshOn401FilterTest {

    private final AutoRefreshOn401Filter filter = new AutoRefreshOn401Filter(WebClient.builder().build(), new RefreshCoordinator());

    @Test
    @DisplayName("Authorization 헤더가 null이어도 NPE 없이 null을 반환한다")
    void extractAtFromAuthHeader_nullSafe() {
        Object result = ReflectionTestUtils.invokeMethod(filter, "extractAtFromAuthHeader", (String) null);
        assertNull(result);
    }

    @Test
    @DisplayName("Bearer 토큰 포맷에서 access token만 추출한다")
    void extractAtFromAuthHeader_extractBearerToken() {
        Object result = ReflectionTestUtils.invokeMethod(filter, "extractAtFromAuthHeader", "Bearer abc.def.ghi");
        assertEquals("abc.def.ghi", result);
    }

    @Test
    @DisplayName("재발급 access token으로 Cookie 헤더의 기존 access token 값을 교체한다")
    void mutateWithNewAT_replaceAccessTokenInCookieHeader() {
        ReflectionTestUtils.setField(filter, "accessCookieName", "AT");

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/test")
                .header(HttpHeaders.COOKIE, "AT=old-token; RT=refresh-token")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        ServerWebExchange mutated = ReflectionTestUtils.invokeMethod(filter, "mutateWithNewAT", exchange, "new-token");

        String cookieHeader = mutated.getRequest().getHeaders().getFirst(HttpHeaders.COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("AT=new-token"));
        assertFalse(cookieHeader.contains("AT=old-token"));
    }
}