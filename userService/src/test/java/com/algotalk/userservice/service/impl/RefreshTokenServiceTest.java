package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.domain.enums.RefreshTokenRotationResult;
import com.algotalk.userservice.service.IRefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

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
        Long userId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String refreshToken = "test.refresh";

        // when
        refreshTokenService.saveRefreshToken(userId, "test-session", refreshToken, Instant.now().plusSeconds(600));

        // then
        String stored = refreshTokenService.getRefreshToken(userId, "test-session");
        assertThat(stored).isEqualTo(refreshToken);

        // cleanup
        refreshTokenService.deleteRefreshToken(userId, "test-session");
    }

    @Test
    @DisplayName("RefreshToken 조회 - 존재하지 않는 경우 null 반환")
    public void getRefreshToken_notExists() throws Exception {
        // given
        Long userId = Math.abs(UUID.randomUUID().getMostSignificantBits());

        // when
        String stored = refreshTokenService.getRefreshToken(userId, "test-session");

        // then
        assertThat(stored).isNull();
    }

    @Test
    @DisplayName("RefreshToken 검증 - 일치하는 경우 true 반환")
    public void validateRefreshToken_match() throws Exception {
        // given
        Long userId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String refreshToken = "test.refresh";

        refreshTokenService.saveRefreshToken(userId, "test-session", refreshToken, Instant.now().plusSeconds(600));

        // when
        boolean result = refreshTokenService.validateRefreshToken(userId, "test-session", refreshToken);

        // then
        assertThat(result).isTrue();

        // cleanup
        refreshTokenService.deleteRefreshToken(userId, "test-session");
    }

    @Test
    @DisplayName("RefreshToken 검증 - 불일치하는 경우 false 반환")
    public void validateRefreshToken_mismatch() throws Exception {
        // given
        Long userId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String refreshToken = "test.refresh";

        refreshTokenService.saveRefreshToken(userId, "test-session", refreshToken, Instant.now().plusSeconds(600));

        // when
        boolean result = refreshTokenService.validateRefreshToken(userId, "test-session", "wrong.refresh");

        // then
        assertThat(result).isFalse();

        // cleanup
        refreshTokenService.deleteRefreshToken(userId, "test-session");
    }

    @Test
    @DisplayName("RefreshToken 교체(RTR) - 기존 토큰 삭제 후 새 토큰 저장")
    public void rotateRefreshToken() throws Exception {
        // given
        Long userId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String oldToken = "old.refresh";
        String newToken = "new.refresh";

        // when
        refreshTokenService.saveRefreshToken(userId, "test-session", oldToken, Instant.now().plusSeconds(600));
        RefreshTokenRotationResult result =
                refreshTokenService.rotateRefreshToken(userId, "test-session", oldToken, newToken, Instant.now().plusSeconds(600));

        String stored = refreshTokenService.getRefreshToken(userId, "test-session");

        // then
        assertThat(result).isEqualTo(RefreshTokenRotationResult.ROTATED);
        assertThat(stored).isEqualTo(newToken);
        assertThat(stored).isNotEqualTo(oldToken);

        // cleanup
        refreshTokenService.deleteRefreshToken(userId, "test-session");
    }

    @Test
    @DisplayName("RefreshToken 삭제 - 삭제 후 null 반환")
    public void deleteRefreshToken() throws Exception {
        // given
        Long userId = Math.abs(UUID.randomUUID().getMostSignificantBits());
        String refreshToken = "test.refresh";

        refreshTokenService.saveRefreshToken(userId, "test-session", refreshToken, Instant.now().plusSeconds(600));

        // when
        refreshTokenService.deleteRefreshToken(userId, "test-session");

        // then
        String stored = refreshTokenService.getRefreshToken(userId, "test-session");
        assertThat(stored).isNull();
    }

}