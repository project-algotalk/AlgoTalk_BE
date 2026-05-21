package com.algotalk.interviewservice.config;

import feign.Logger.Level;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenFeignConfig {

    private static final int CONNECT_TIMEOUT_MS = 5000;   // 연결 타임아웃 5초
    private static final int READ_TIMEOUT_MS = 30000;      // 읽기 타임아웃 30초 (LLM 응답 대기)

    // aiService 호출 타임아웃 설정
    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS);
    }

    // Feign 클라이언트 로그 레벨 설정 (BASIC: 요청/응답 상태코드, URL만 로깅)
    @Bean
    Level feignLoggerLevel() {
        return Level.BASIC;
    }
}
