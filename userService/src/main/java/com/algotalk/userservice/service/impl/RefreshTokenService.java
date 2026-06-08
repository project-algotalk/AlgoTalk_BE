package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.domain.enums.RefreshTokenRotationResult;
import com.algotalk.userservice.persistence.IRedisMapper;
import com.algotalk.userservice.service.IRefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 로그인 세션별 Refresh Token과 사용자별 활성 세션 인덱스 관리
 * 기존 refresh:{userId} key와 sessionId가 없는 RT는 조회 대상에서 제외하며 배포 후 재로그인 필요
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService implements IRefreshTokenService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";
    private static final String SESSION_INDEX_KEY_PREFIX = "refresh:sessions:";

    private final IRedisMapper redisMapper;

    @Override
    public void saveRefreshToken(Long userId, String sessionId, String refreshToken, Instant expiresAt) {
        log.info("{}.saveRefreshToken Start!", this.getClass().getName());

        long ttlMillis = remainingTtlMillis(expiresAt);
        String key = key(userId, sessionId);
        redisMapper.setValue(key, refreshToken, ttlMillis, TimeUnit.MILLISECONDS);
        redisMapper.addSetMember(indexKey(userId), sessionId);
        log.info("Refresh Token 저장 완료: key={}", key);

        log.info("{}.saveRefreshToken End!", this.getClass().getName());
    }

    @Override
    public String getRefreshToken(Long userId, String sessionId) {
        log.info("{}.getRefreshToken Start!", this.getClass().getName());

        String key = key(userId, sessionId);
        String refreshToken = redisMapper.getValue(key);
        log.info("Refresh Token 조회: key={}", key);

        log.info("{}.getRefreshToken End!", this.getClass().getName());
        return refreshToken;
    }

    @Override
    public void deleteRefreshToken(Long userId, String sessionId) {
        log.info("{}.deleteRefreshToken Start!", this.getClass().getName());

        redisMapper.delete(key(userId, sessionId));
        redisMapper.removeSetMember(indexKey(userId), sessionId);
        log.info("Refresh Token 세션 삭제: userId={}, sessionId={}", userId, sessionId);

        log.info("{}.deleteRefreshToken End!", this.getClass().getName());
    }

    @Override
    public void deleteAllRefreshTokens(Long userId) {
        log.info("{}.deleteAllRefreshTokens Start!", this.getClass().getName());

        Set<String> sessionIds = redisMapper.getSetMembers(indexKey(userId));
        if (sessionIds != null) {
            sessionIds.forEach(sessionId -> redisMapper.delete(key(userId, sessionId)));
        }
        redisMapper.delete(indexKey(userId));
        log.info("사용자의 전체 Refresh Token 세션 삭제: userId={}", userId);

        log.info("{}.deleteAllRefreshTokens End!", this.getClass().getName());
    }

    @Override
    public boolean validateRefreshToken(Long userId, String sessionId, String refreshToken) {
        log.info("{}.validateRefreshToken Start!", this.getClass().getName());

        String storedToken = getRefreshToken(userId, sessionId);
        boolean valid = storedToken != null && storedToken.equals(refreshToken);
        if (valid) {
            log.info("Refresh Token 검증 성공: userId={}, sessionId={}", userId, sessionId);
        }

        log.info("{}.validateRefreshToken End!", this.getClass().getName());
        return valid;
    }

    @Override
    public RefreshTokenRotationResult rotateRefreshToken(
            Long userId,
            String sessionId,
            String expectedRefreshToken,
            String newRefreshToken,
            Instant expiresAt
    ) {
        log.info("{}.rotateRefreshToken Start!", this.getClass().getName());

        String key = key(userId, sessionId);
        IRedisMapper.CompareAndSetResult result = redisMapper.compareAndSet(
                key,
                expectedRefreshToken,
                newRefreshToken,
                remainingTtlMillis(expiresAt),
                TimeUnit.MILLISECONDS
        );

        RefreshTokenRotationResult rotationResult;
        if (result == IRedisMapper.CompareAndSetResult.UPDATED) {
            log.info("Refresh Token RTR 완료: key={}", key);
            rotationResult = RefreshTokenRotationResult.ROTATED;
        } else if (result == IRedisMapper.CompareAndSetResult.NOT_FOUND) {
            rotationResult = RefreshTokenRotationResult.NOT_FOUND;
        } else {
            rotationResult = RefreshTokenRotationResult.MISMATCH;
        }

        log.info("{}.rotateRefreshToken End!", this.getClass().getName());
        return rotationResult;
    }

    private long remainingTtlMillis(Instant expiresAt) {
        long ttlMillis = Duration.between(Instant.now(), expiresAt).toMillis();
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("만료된 Refresh Token은 Redis에 저장할 수 없습니다.");
        }
        return ttlMillis;
    }

    private String key(Long userId, String sessionId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId + ":" + sessionId;
    }

    private String indexKey(Long userId) {
        return SESSION_INDEX_KEY_PREFIX + userId;
    }
}