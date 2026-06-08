package com.algotalk.userservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@EnableCaching
@Configuration
public class CacheConfig {

    /**
     * Spring Cache(Redis) 기본 설정
     * - 키: 문자열(StringRedisSerializer)
     * - 값: JDK 직렬화(JdkSerializationRedisSerializer)
     * - 캐시 만료시간: 3시간
     * - 주의: 캐시되는 객체와 중첩 객체는 Serializable을 구현해야 함
     * @param cf
     * @return
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory cf) {

        RedisCacheConfiguration conf = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new JdkSerializationRedisSerializer())
                )
                .entryTtl(Duration.ofHours(3))
                .disableCachingNullValues();

        return RedisCacheManager.builder(cf)
                .cacheDefaults(conf)
                .build();
    }
}
