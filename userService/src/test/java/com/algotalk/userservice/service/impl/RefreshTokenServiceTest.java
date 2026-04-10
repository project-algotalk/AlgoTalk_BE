package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.service.IRefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@SpringBootTest
@ActiveProfiles("local")
class RefreshTokenServiceTest {

    @Autowired
    IRefreshTokenService refreshTokenService;

    @Test
    @DisplayName("RefreshToken 저장 - Redis에 정상 저장 확인")
    void saveRefreshToken_success() throws Exception {
        // given
        Long userId = System.currentTimeMillis(); // 고유한 userId 생성
        String refreshToken = "test.refresh";

        // when
        refreshTokenService.saveRefreshToken(userId, refreshToken);

        // then
        String stored = refreshTokenService.getRefreshToken(userId);
        assertThat(stored).isEqualTo(refreshToken);

        // cleanup
        refreshTokenService.deleteRefreshToken(userId);
    }

    @Test
    @DisplayName("RefreshToken 조회 - 존재하지 않는 경우 null 반환")
    public void getRefreshToken_notExists() throws Exception {
        // given
        Long userId = System.currentTimeMillis();

        // when
        String stored = refreshTokenService.getRefreshToken(userId);

        // then
        assertThat(stored).isNull();
    }

    @Test
    @DisplayName("RefreshToken 검증 - 일치하는 경우 true 반환")
    public void validateRefreshToken_match() throws Exception {
        // given
        Long userId = System.currentTimeMillis();
        String refreshToken = "test.refresh";

        refreshTokenService.saveRefreshToken(userId, refreshToken);

        // when
        boolean result = refreshTokenService.validateRefreshToken(userId, refreshToken);

        // then
        assertThat(result).isTrue();

        // cleanup
        refreshTokenService.deleteRefreshToken(userId);
    }

    @Test
    @DisplayName("RefreshToken 검증 - 불일치하는 경우 false 반환")
    public void validateRefreshToken_mismatch() throws Exception {
        // given
        Long userId = System.currentTimeMillis();
        String refreshToken = "test.refresh";

        refreshTokenService.saveRefreshToken(userId, refreshToken);

        // when
        boolean result = refreshTokenService.validateRefreshToken(userId, "wrong.refresh");

        // then
        assertThat(result).isFalse();

        // cleanup
        refreshTokenService.deleteRefreshToken(userId);
    }

    @Test
    @DisplayName("RefreshToken 교체(RTR) - 기존 토큰 삭제 후 새 토큰 저장")
    public void rotateRefreshToken() throws Exception {
        // given
        Long userId = System.currentTimeMillis();
        String oldToken = "old.refresh";
        String newToken = "new.refresh";

        // when
        refreshTokenService.saveRefreshToken(userId, oldToken);
        refreshTokenService.rotateRefreshToken(userId, newToken);

        String stored = refreshTokenService.getRefreshToken(userId);

        // then
        assertThat(stored).isEqualTo(newToken);
        assertThat(stored).isNotEqualTo(oldToken);

        // cleanup
        refreshTokenService.deleteRefreshToken(userId);
    }

    @Test
    @DisplayName("RefreshToken 삭제 - 삭제 후 null 반환")
    public void deleteRefreshToken() throws Exception {
        // given
        Long userId = System.currentTimeMillis();
        String refreshToken = "test.refresh";

        refreshTokenService.saveRefreshToken(userId, refreshToken);

        // when
        refreshTokenService.deleteRefreshToken(userId);

        // then
        String stored = refreshTokenService.getRefreshToken(userId);
        assertThat(stored).isNull();
    }

}