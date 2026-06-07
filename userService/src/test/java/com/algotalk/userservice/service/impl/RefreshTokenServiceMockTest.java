package com.algotalk.userservice.service.impl;

import com.algotalk.userservice.persistence.IRedisMapper;
import com.algotalk.userservice.service.IRefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceMockTest {

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Mock
    private IRedisMapper redisMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpiration", 604_800_000L);
    }

    @Test
    @DisplayName("RTR은 Redis 원자적 compare-and-set 결과를 반환한다")
    void rotateRefreshTokenUsesAtomicCompareAndSet() {
        given(redisMapper.compareAndSet(
                "refresh:1", "old-rt", "new-rt", 604_800_000L, TimeUnit.MILLISECONDS
        )).willReturn(IRedisMapper.CompareAndSetResult.UPDATED);

        IRefreshTokenService.RotationResult result =
                refreshTokenService.rotateRefreshToken(1L, "old-rt", "new-rt");

        assertThat(result).isEqualTo(IRefreshTokenService.RotationResult.ROTATED);
        verify(redisMapper).compareAndSet(
                "refresh:1", "old-rt", "new-rt", 604_800_000L, TimeUnit.MILLISECONDS
        );
    }

    @Test
    @DisplayName("저장된 RT 불일치는 MISMATCH로 변환한다")
    void rotateRefreshTokenReturnsMismatch() {
        given(redisMapper.compareAndSet(
                "refresh:1", "stale-rt", "new-rt", 604_800_000L, TimeUnit.MILLISECONDS
        )).willReturn(IRedisMapper.CompareAndSetResult.MISMATCH);

        IRefreshTokenService.RotationResult result =
                refreshTokenService.rotateRefreshToken(1L, "stale-rt", "new-rt");

        assertThat(result).isEqualTo(IRefreshTokenService.RotationResult.MISMATCH);
    }
}