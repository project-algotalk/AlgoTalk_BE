package com.algotalk.apigateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

class AutoRefreshOn401FilterTest {

    private final AutoRefreshOn401Filter filter = new AutoRefreshOn401Filter(WebClient.builder().build());

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
}