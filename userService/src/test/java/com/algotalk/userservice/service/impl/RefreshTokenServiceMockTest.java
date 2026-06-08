package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.domain.enums.RefreshTokenRotationResult;
import com.algotalk.userservice.persistence.IRedisMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceMockTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private IRedisMapper redisMapper;

    @Test
    @DisplayName("동일 사용자의 로그인 세션별 Refresh Token key 분리")
    void storesEachLoginInItsOwnSessionKey() {
        Instant expiresAt = Instant.now().plusSeconds(600);

        refreshTokenService.saveRefreshToken(1L, "pc", "pc-rt", expiresAt);
        refreshTokenService.saveRefreshToken(1L, "mobile", "mobile-rt", expiresAt);

        verify(redisMapper).setValue(eq("refresh:1:pc"), eq("pc-rt"), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(redisMapper).setValue(eq("refresh:1:mobile"), eq("mobile-rt"), anyLong(), eq(TimeUnit.MILLISECONDS));
        verify(redisMapper).addSetMember("refresh:sessions:1", "pc");
        verify(redisMapper).addSetMember("refresh:sessions:1", "mobile");
    }

    @Test
    @DisplayName("세션 단위 Refresh Token 회전 시 Redis 원자적 CAS 사용")
    void rotateRefreshTokenUsesSessionScopedAtomicCompareAndSet() {
        Instant expiresAt = Instant.now().plusSeconds(600);
        given(redisMapper.compareAndSet(
                eq("refresh:1:session-a"),
                eq("old-rt"),
                eq("new-rt"),
                anyLong(),
                eq(TimeUnit.MILLISECONDS)
        )).willReturn(IRedisMapper.CompareAndSetResult.UPDATED);

        RefreshTokenRotationResult result = refreshTokenService.rotateRefreshToken(
                1L, "session-a", "old-rt", "new-rt", expiresAt
        );

        assertThat(result).isEqualTo(RefreshTokenRotationResult.ROTATED);
    }

    @Test
    @DisplayName("사용자 세션 인덱스를 이용한 전체 Refresh Token 삭제")
    void deleteAllRefreshTokensUsesUserSessionIndex() {
        given(redisMapper.getSetMembers("refresh:sessions:1")).willReturn(Set.of("pc", "mobile"));

        refreshTokenService.deleteAllRefreshTokens(1L);

        verify(redisMapper).delete("refresh:1:pc");
        verify(redisMapper).delete("refresh:1:mobile");
        verify(redisMapper).delete("refresh:sessions:1");
    }
}