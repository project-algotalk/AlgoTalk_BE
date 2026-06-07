package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.persistence.IRedisMapper;
import com.algotalk.userservice.service.IRefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token의 Redis key와 만료 정책을 관리하는 서비스
 * 실제 Redis 명령은 IRedisMapper에 위임하여 서비스 로직과 저장소 구현을 분리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService implements IRefreshTokenService {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";

    // 서비스 계층에서 StringRedisTemplate을 직접 사용하지 않고 Mapper를 통해 접근
    private final IRedisMapper redisMapper;

    @Value("${jwt.refresh.token.expiration}")
    private long refreshTokenExpiration;

    @Override
    public void saveRefreshToken(Long userId, String refreshToken) {
        log.info("{}.saveRefreshToken Start!", this.getClass().getName());

        String key = key(userId);
        redisMapper.setValue(key, refreshToken, refreshTokenExpiration, TimeUnit.MILLISECONDS);
        log.info("Refresh Token 저장 완료: key={}", key);

        log.info("{}.saveRefreshToken End!", this.getClass().getName());
    }

    @Override
    public String getRefreshToken(Long userId) {
        log.info("{}.getRefreshToken Start!", this.getClass().getName());

        String key = key(userId);
        String refreshToken = redisMapper.getValue(key);
        log.info("Refresh Token 조회: key={}", key);

        log.info("{}.getRefreshToken End!", this.getClass().getName());
        return refreshToken;
    }

    @Override
    public void deleteRefreshToken(Long userId) {
        log.info("{}.deleteRefreshToken Start!", this.getClass().getName());

        String key = key(userId);
        redisMapper.delete(key);
        log.info("Refresh Token 삭제: key={}", key);

        log.info("{}.deleteRefreshToken End!", this.getClass().getName());
    }

    @Override
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        log.info("{}.validateRefreshToken Start!", this.getClass().getName());

        String key = key(userId);
        String storedToken = redisMapper.getValue(key);
        log.info("Refresh Token 검증: key={}", key);

        boolean valid = storedToken != null && storedToken.equals(refreshToken);
        if (valid) {
            log.info("Refresh Token 검증 성공!");
        }

        log.info("{}.validateRefreshToken End!", this.getClass().getName());
        return valid;
    }

    @Override
    public RotationResult rotateRefreshToken(
            Long userId,
            String expectedRefreshToken,
            String newRefreshToken
    ) {
        log.info("{}.rotateRefreshToken Start!", this.getClass().getName());

        String key = key(userId);
        // 기존 RT 확인과 새 RT 저장을 Redis Lua CAS 한 번으로 처리
        IRedisMapper.CompareAndSetResult result = redisMapper.compareAndSet(
                key,
                expectedRefreshToken,
                newRefreshToken,
                refreshTokenExpiration,
                TimeUnit.MILLISECONDS
        );

        // 저장소 계층의 결과를 인증 도메인에서 사용하는 RotationResult로 변환
        RotationResult rotationResult = switch (result) {
            case UPDATED -> {
                log.info("Refresh Token RTR 완료: key={}", key);
                yield RotationResult.ROTATED;
            }
            case NOT_FOUND -> RotationResult.NOT_FOUND;
            case MISMATCH -> RotationResult.MISMATCH;
        };

        log.info("{}.rotateRefreshToken End!", this.getClass().getName());
        return rotationResult;
    }

    // 현재 저장 구조는 사용자별 RT 한 개를 refresh:{userId} 형식으로 관리
    private String key(Long userId) {
        return REFRESH_TOKEN_KEY_PREFIX + userId;
    }
}