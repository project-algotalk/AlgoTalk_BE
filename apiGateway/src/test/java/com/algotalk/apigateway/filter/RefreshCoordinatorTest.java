package com.algotalk.apigateway.filter;

import com.algotalk.apigateway.auth.refresh.RefreshCoordinator;
import com.algotalk.apigateway.auth.refresh.RefreshOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshCoordinatorTest {

    private RefreshCoordinator coordinator;

    @BeforeEach
    void setUp() {
        coordinator = new RefreshCoordinator();
        ReflectionTestUtils.setField(coordinator, "graceDuration", Duration.ofMillis(200));
    }

    @Test
    @DisplayName("동일 RT의 동시 재발급은 하나의 Mono를 공유한다")
    void sameRefreshTokenSharesSingleFlight() {
        AtomicInteger calls = new AtomicInteger();
        RefreshOutcome expected = new RefreshOutcome("new-at", List.of("RT=new-rt"));

        Mono<RefreshOutcome> first = coordinator.refresh("old-rt", () -> delayedRefresh(calls, expected));
        Mono<RefreshOutcome> second = coordinator.refresh("old-rt", () -> delayedRefresh(calls, expected));

        StepVerifier.create(Mono.zip(first, second))
                .assertNext(results -> {
                    assertThat(results.getT1()).isEqualTo(expected);
                    assertThat(results.getT2()).isEqualTo(expected);
                })
                .verifyComplete();

        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("성공 직후 같은 RT 요청은 grace window 동안 성공 결과를 재사용한다")
    void recentSuccessIsReusedDuringGraceWindow() {
        AtomicInteger calls = new AtomicInteger();
        RefreshOutcome expected = new RefreshOutcome("new-at", List.of("RT=new-rt"));

        StepVerifier.create(coordinator.refresh("old-rt", () -> delayedRefresh(calls, expected)))
                .expectNext(expected)
                .verifyComplete();

        StepVerifier.create(coordinator.refresh("old-rt", () -> delayedRefresh(calls, expected)))
                .expectNext(expected)
                .verifyComplete();

        assertThat(calls).hasValue(1);
    }

    @Test
    @DisplayName("실패한 재발급 결과는 grace cache에 저장하지 않는다")
    void failureIsNotCached() {
        AtomicInteger calls = new AtomicInteger();

        StepVerifier.create(coordinator.refresh("old-rt", () -> {
                    calls.incrementAndGet();
                    return Mono.empty();
                }))
                .verifyComplete();

        StepVerifier.create(coordinator.refresh("old-rt", () -> {
                    calls.incrementAndGet();
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(calls).hasValue(2);
    }

    private Mono<RefreshOutcome> delayedRefresh(
            AtomicInteger calls,
            RefreshOutcome outcome
    ) {
        return Mono.defer(() -> {
            calls.incrementAndGet();
            return Mono.just(outcome).delayElement(Duration.ofMillis(50));
        });
    }
}