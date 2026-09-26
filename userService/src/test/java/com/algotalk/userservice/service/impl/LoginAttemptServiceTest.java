package com.algotalk.userservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginAttemptService, "maxFailCount", 5);
        ReflectionTestUtils.setField(loginAttemptService, "lockMinutes", 10L);

        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("계정 잠금 여부 조회 성공 - 잠금 상태")
    void isLocked_success_locked() {
        // given
        given(stringRedisTemplate.hasKey("login:lock:user01")).willReturn(true);

        // when
        boolean result = loginAttemptService.isLocked("user01");

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("계정 잠금 여부 조회 성공 - 잠금 아님")
    void isLocked_success_notLocked() {
        // given
        given(stringRedisTemplate.hasKey("login:lock:user01")).willReturn(false);

        // when
        boolean result = loginAttemptService.isLocked("user01");

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("여러 계정 잠금 여부 조회 성공")
    void getLockStatuses_success() {
        // given
        List<String> loginIds = List.of("user01", "user02");
        given(valueOperations.multiGet(List.of(
                "login:lock:user01",
                "login:lock:user02"
        ))).willReturn(java.util.Arrays.asList("Y", null));

        // when
        Map<String, Boolean> result = loginAttemptService
                .getLockStatuses(loginIds);

        // then
        assertThat(result)
                .containsEntry("user01", true)
                .containsEntry("user02", false);
    }

    @Test
    @DisplayName("여러 계정 잠금 여부 조회 성공 - 회원 목록 없음")
    void getLockStatuses_success_empty() {
        // when
        Map<String, Boolean> result = loginAttemptService
                .getLockStatuses(List.of());

        // then
        assertThat(result).isEmpty();
        verify(valueOperations, never()).multiGet(anyList());
    }

    @Test
    @DisplayName("로그인 실패 처리 성공 - 제한 횟수 미만")
    void recordFailure_success_belowLimit() {
        // given
        given(valueOperations.increment("login:fail:user01")).willReturn(4L);

        // when
        loginAttemptService.recordFailure("user01");

        // then
        verify(stringRedisTemplate).expire("login:fail:user01", 10L, TimeUnit.MINUTES);
        verify(valueOperations, never()).set(
                anyString(), anyString(), anyLong(), any(TimeUnit.class)
        );
    }

    @Test
    @DisplayName("로그인 실패 처리 성공 - 제한 횟수 도달 시 계정 잠금")
    void recordFailure_success_lockAtLimit() {
        // given
        given(valueOperations.increment("login:fail:user01")).willReturn(5L);

        // when
        loginAttemptService.recordFailure("user01");

        // then
        verify(valueOperations).set("login:lock:user01", "Y", 10L, TimeUnit.MINUTES);
    }

    @Test
    @DisplayName("로그인 시도 초기화 성공")
    void resetAttempts_success() {
        // when
        loginAttemptService.resetAttempts("user01");

        // then
        verify(stringRedisTemplate).delete(List.of(
                "login:fail:user01",
                "login:lock:user01"
        ));
    }

    @Test
    @DisplayName("계정 잠금 남은 시간 조회 성공")
    void getRemainingLockSeconds_success() {
        // given
        given(stringRedisTemplate.getExpire(
                "login:lock:user01", TimeUnit.SECONDS
        )).willReturn(120L);

        // when
        long result = loginAttemptService.getRemainingLockSeconds("user01");

        // then
        assertThat(result).isEqualTo(120L);
    }

    @Test
    @DisplayName("계정 잠금 남은 시간 조회 성공 - 잠금 정보 없음")
    void getRemainingLockSeconds_success_noLock() {
        // given
        given(stringRedisTemplate.getExpire(
                "login:lock:user01", TimeUnit.SECONDS
        )).willReturn(-2L);

        // when
        long result = loginAttemptService.getRemainingLockSeconds("user01");

        // then
        assertThat(result).isZero();
    }
}
