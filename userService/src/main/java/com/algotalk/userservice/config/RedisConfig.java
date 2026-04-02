package com.algotalk.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 설정
 *
 * StringRedisTemplate 사용 -> RtSession을 JSON 문자열로 변환해서 저장
 * (ObjectMapper로 직렬화/역직렬화는 Service 레이어에서 처리)
 *
 * Redis Key 패턴: rtsid:{userId}:{handle}
 * TTL: RefreshTokenService에서 jwt.refresh.token.expiration 으로 적용
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    // Redis 인증 설정한 경우만 사용. 없으면 빈 문자열
    @Value("${spring.data.redis.username:}")
    private String redisUsername;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Redis 서버와의 실제 연결을 관리하는 커넥션 팩토리
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        if (!redisUsername.isBlank()) config.setUsername(redisUsername);
        if (!redisPassword.isBlank()) config.setPassword(redisPassword);
        return new LettuceConnectionFactory(config);
    }

    /**
     * Object 타입 데이터를 저장할 때 사용하는 범용 템플릿
     * - String 이외의 데이터를 저장할 때 사용
     * - RT 저장은 아래의 StringRedisTemplate 을 사용
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory());
        return template;
    }

    /**
     * JSON 문자열 저장에 최적화된 템플릿
     * RtSession -> ObjectMapper.writeValueAsString() -> Redis 저장
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }
}