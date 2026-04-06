package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.service.IRefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService implements IRefreshTokenService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String REFRESH_TOKEN_KEY_PREFIX = "refresh:";

    @Value("${jwt.refresh.token.expiration}")
    private long refreshTokenExpiration; // 7일 (밀리초 단위)

    @Override
    public void saveRefreshToken(Long userId, String refreshToken) throws Exception {
        log.info("{}.saveRefreshToken Start!", this.getClass().getName());

        String key = REFRESH_TOKEN_KEY_PREFIX + userId;
        // ms -> 초 변환
        long expirationSeconds = refreshTokenExpiration / 1000;
        stringRedisTemplate.opsForValue().set(key, refreshToken, expirationSeconds);
        log.info("Refresh Token 저장: key={}, value={}, expiration={}초",
                key, refreshToken, expirationSeconds);

        log.info("{}.saveFreshToken End!", this.getClass().getName());
    }

    @Override
    public String getRefreshToken(Long userId) throws Exception {
        log.info("{}.getRefreshToken Start!", this.getClass().getName());

        String key = REFRESH_TOKEN_KEY_PREFIX + userId;
        String refreshToken = stringRedisTemplate.opsForValue().get(key);
        log.info("Refresh Token 조회: key={}, value={}", key, refreshToken);

        log.info("{}.getRefreshToken End!", this.getClass().getName());
        return refreshToken;
    }

    @Override
    public void deleteRefreshToken(Long userId) throws Exception {
        log.info("{}.deleteRefreshToken Start!", this.getClass().getName());

        String key = REFRESH_TOKEN_KEY_PREFIX + userId;
        stringRedisTemplate.delete(key);
        log.info("Refresh Token 삭제: key={}", key);

        log.info("{}.deleteRefreshToken End!", this.getClass().getName());
    }

    @Override
    public boolean validateRefreshToken(Long userId, String refreshToken) throws Exception {
        log.info("{}.validateRefreshToken Start!", this.getClass().getName());

        String key = REFRESH_TOKEN_KEY_PREFIX + userId;
        String storedToken = stringRedisTemplate.opsForValue().get(key);
        log.info("Refresh Token 검증: key={}, storedToken={}, providedToken={}",
                key, storedToken, refreshToken);
        if(storedToken != null && storedToken.equals(refreshToken)) {
            log.info("Refresh Token 검증 성공!");
            return true;
        }
        return false;
    }
}
