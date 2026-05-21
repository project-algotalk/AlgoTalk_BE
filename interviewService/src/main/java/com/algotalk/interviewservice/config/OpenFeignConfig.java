package com.algotalk.interviewservice.config;

import feign.Logger.Level;
import feign.Request;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenFeignConfig {

    // aiService 호출 타임아웃 설정 (LLM 응답 대기 시간 고려)
    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(5000, 30000);
    }

    // Feign 클라이언트 로그 레벨 설정
    @Bean
    Level feignLoggerLevel() {
        return Level.FULL;
    }
}
