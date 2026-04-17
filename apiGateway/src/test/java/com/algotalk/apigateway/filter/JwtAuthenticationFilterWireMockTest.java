package com.algotalk.apigateway.filter;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;
import java.util.Date;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT) // 실제 애플리케이션 컨텍스트를 로드하여 테스트
@ActiveProfiles("test")
@AutoConfigureWebTestClient
class JwtAuthenticationFilterWireMockTest {

    @Autowired
    private WebTestClient webTestClient;

    @Value("${spring.security.oauth2.resourceserver.jwt.secret-key}")
    private String secretKey;

    @Test
    @DisplayName("permitAll - 인증 없이 필터 통과 성공")
    void permitAll_filter_pass() {
        // given
        WireMockServer wireMockServer = new WireMockServer(9999); // 가짜 서버 생성
        wireMockServer.start();

        // 가짜 응답 정의
        wireMockServer.stubFor(post(urlEqualTo("/user/v1/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("OK")));

        // when & then
        webTestClient.post()
                .uri("/api/user/v1/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    {
                      "loginId": "algotalk",
                      "password": "test1234!"
                    }
                """)
                .exchange()
                .expectStatus().isOk();

        // cleanup
        wireMockServer.stop();
    }
}