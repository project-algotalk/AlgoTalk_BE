package com.algotalk.userservice.persistence.iml;

import com.algotalk.userservice.persistence.IRedisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * User Service의 문자열 Redis 연산을 담당하는 Mapper 구현체
 */
@Component
@RequiredArgsConstructor
public class RedisMapper implements IRedisMapper {

    // Lua script가 반환하는 숫자를 의미 있는 결과로 변환하기 위한 상수
    private static final long UPDATED = 1L;
    private static final long NOT_FOUND = 0L;

    /**
     * Refresh Token Rotation에 사용하는 원자적 compare-and-set script
     *
     * 1. Redis에 현재 RT가 있는지 확인
     * 2. 요청이 제출한 기존 RT와 Redis RT가 같은지 확인
     * 3. 같을 때에만 새 RT와 새 TTL을 한 번에 저장
     *
     * Redis는 Lua script 전체를 중간에 다른 명령이 끼어들지 않게 실행하므로,
     * 동시에 두 요청이 들어와도 동일한 기존 RT는 최대 한 번만 교체에 성공함
     */
    private static final DefaultRedisScript<Long> COMPARE_AND_SET_SCRIPT =
            new DefaultRedisScript<>("""
                    local current = redis.call('GET', KEYS[1])
                    if not current then
                        return 0
                    end
                    if current ~= ARGV[1] then
                        return -1
                    end
                    redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                    return 1
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void setValue(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    @Override
    public CompareAndSetResult compareAndSet(
            String key,
            String expectedValue,
            String newValue,
            long timeout,
            TimeUnit unit
    ) {
        // Lua의 PX 옵션은 밀리초를 사용하므로 호출자가 넘긴 단위를 밀리초로 통일
        long timeoutMillis = unit.toMillis(timeout);

        // KEYS[1]에는 Redis key, ARGV에는 기존 RT, 새 RT, 새 TTL 순서로 전달
        Long result = redisTemplate.execute(
                COMPARE_AND_SET_SCRIPT,
                Collections.singletonList(key),
                expectedValue,
                newValue,
                String.valueOf(timeoutMillis)
        );

        // Redis 숫자 결과를 서비스 계층이 처리하기 쉬운 enum으로 바꿈
        if (result != null && result == UPDATED) {
            return CompareAndSetResult.UPDATED;
        }
        if (result != null && result == NOT_FOUND) {
            return CompareAndSetResult.NOT_FOUND;
        }

        // -1 또는 예상하지 못한 null 결과는 안전하게 불일치로 처리하여 교체를 허용하지 않음
        return CompareAndSetResult.MISMATCH;
    }
}