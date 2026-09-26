package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.service.ILoginAttemptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginAttemptService
        implements ILoginAttemptService {

    private static final String LOGIN_FAIL_KEY =
            "login:fail:";

    private static final String LOGIN_LOCK_KEY =
            "login:lock:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${login.max-fail-count}")
    private int maxFailCount;

    @Value("${login.lock-minutes}")
    private long lockMinutes;

    @Override
    public boolean isLocked(String loginId) {
        String lockKey = LOGIN_LOCK_KEY + loginId;

        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(lockKey));
    }

    @Override
    public void recordFailure(String loginId) {
        String failKey = LOGIN_FAIL_KEY + loginId;

        Long failCount = stringRedisTemplate
                .opsForValue()
                .increment(failKey);

        stringRedisTemplate.expire(
                failKey,
                lockMinutes,
                TimeUnit.MINUTES
        );

        log.info("로그인 실패 횟수 증가: loginId={}, failCount={}", loginId, failCount);

        if (failCount != null && failCount >= maxFailCount) {
            lock(loginId);
        }
    }

    @Override
    public void resetAttempts(String loginId) {
        stringRedisTemplate.delete(List.of(
                LOGIN_FAIL_KEY + loginId,
                LOGIN_LOCK_KEY + loginId
        ));

        log.info("로그인 실패 횟수 및 잠금 상태 초기화: loginId={}", loginId);
    }

    @Override
    public long getRemainingLockSeconds(
            String loginId
    ) {
        Long remainingSeconds =
                stringRedisTemplate.getExpire(
                        LOGIN_LOCK_KEY + loginId,
                        TimeUnit.SECONDS
                );

        if (remainingSeconds == null || remainingSeconds < 0) {
            return 0L;
        }

        return remainingSeconds;
    }

    private void lock(String loginId) {
        String lockKey = LOGIN_LOCK_KEY + loginId;

        stringRedisTemplate.opsForValue().set(
                lockKey,
                "Y",
                lockMinutes,
                TimeUnit.MINUTES
        );

        log.warn("계정 잠금 처리: loginId={}, lockMinutes={}", loginId, lockMinutes);
    }
}