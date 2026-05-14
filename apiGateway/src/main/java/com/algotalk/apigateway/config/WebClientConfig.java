package com.algotalk.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {
        // WebClient 설정을 위한 Bean 등록
        // 예시: WebClient.Builder를 Bean으로 등록하여 다른 서비스에서 주입받아 사용할 수 있도록 함
        @Bean
        public WebClient webClient(WebClient.Builder builder) {
            return builder.build();
        }
}
