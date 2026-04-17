package com.algotalk.apigateway.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class JwtAuthenticationFilterFailTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("TOKEN_NOT_FOUND - Authorization 헤더 없음")
    void no_token() {
        webTestClient.get()
                .uri("/api/user/v1/logout")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("INVALID_TOKEN - 올바르지 않은 토큰")
    void invalid_token() {
        webTestClient.get()
                .uri("/api/user/v1/logout")
                .header("Authorization", "Bearer invalid-token")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}