package com.algotalk.apigateway.auth.refresh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 같은 RT로 들어온 여러 재발급 요청을 하나의 작업으로 합치는 조정자
 *
 * 예를 들어 Promise.all로 요청 A와 B가 동시에 401을 받더라도,
 * 먼저 들어온 요청만 User Service를 호출하고 나머지 요청은 같은 Mono 결과를 기다리게 함
 *
 * 단일 Gateway 인스턴스 안에서만 공유되는 메모리 기반 Single Flight 구현
 */
@Slf4j
@Component
public class RefreshCoordinator {

    // 현재 실행 중인 재발급 작업
    // 같은 RT의 follower 요청은 이 Mono를 함께 구독하여 결과를 공유
    private final ConcurrentMap<String, Mono<RefreshOutcome>> inFlight = new ConcurrentHashMap<>();

    // 첫 재발급 직후 old RT로 늦게 도착한 요청이 같은 성공 결과를 사용할 수 있게 잠시 보관
    private final ConcurrentMap<String, CachedOutcome> recentSuccess = new ConcurrentHashMap<>();

    // 성공 결과 재사용 시간이다. 설정이 없으면 2초 동안만 유지
    // (2초는 실제로 2초를 기다리는 것이 아니라, 재발급 작업이 끝난 시점부터 2초 동안만 최근 성공 결과를 재사용한다는 의미)
    @Value("${jwt.refresh.single-flight.grace:2s}")
    private Duration graceDuration;

    /**
     * 동일 RT에 대해 최근 성공 결과, 진행 중 작업 순서로 확인
     * 둘 다 없을 때에만 실제 재발급 작업을 새로 만듦
     */
    public Mono<RefreshOutcome> refresh(String refreshToken, Supplier<Mono<RefreshOutcome>> refreshAction) {
        // RT 원문을 Map key로 남기지 않도록 SHA-256 사용
        String key = fingerprint(refreshToken);

        // 재발급 직후의 후발 요청이면 User Service를 다시 호출하지 않고 직전 성공 결과를 반환
        RefreshOutcome cached = findRecentSuccess(key);
        if (cached != null) {
            log.debug("[AutoRefresh] 최근 재발급 결과 재사용: key={}", abbreviated(key));
            return Mono.just(cached);
        }

        // computeIfAbsent가 같은 key에 대해 하나의 공유 Mono만 등록하므로 중복 재발급을 막음
        return inFlight.computeIfAbsent(key, ignored -> createSharedRefresh(key, refreshAction));
    }

    private Mono<RefreshOutcome> createSharedRefresh(
            String key,
            Supplier<Mono<RefreshOutcome>> refreshAction
    ) {
        // 작업 종료 시 Map에서 정확히 자기 자신만 제거하기 위해 공유 Mono 참조를 보관
        AtomicReference<Mono<RefreshOutcome>> self = new AtomicReference<>();

        Mono<RefreshOutcome> shared = Mono.defer(refreshAction)
                // 실제 성공 값이 나온 경우에만 grace cache에 저장. 오류와 empty는 저장하지 않음
                .doOnNext(outcome -> saveRecentSuccess(key, outcome))
                // 성공, 실패, 취소 여부와 관계없이 종료된 작업을 in-flight Map에서 정리
                .doFinally(signal -> inFlight.remove(key, self.get()))
                // 여러 subscriber가 구독해도 실제 User Service 호출은 한 번만 실행하고 결과를 공유
                .cache();

        // 공유 Mono 참조를 저장하여 doFinally에서 정확히 자기 자신을 제거할 수 있게 함
        self.set(shared);
        log.debug("[AutoRefresh] 새 재발급 Single Flight 시작: key={}", abbreviated(key));
        return shared;
    }

    /**
     * 아직 grace 시간이 남은 성공 결과만 반환
     * 조회 시 이미 만료된 항목도 즉시 제거하여 오래된 토큰 결과가 재사용되지 않게 함
     */
    private RefreshOutcome findRecentSuccess(String key) {
        CachedOutcome cached = recentSuccess.get(key);
        if (cached == null) {
            return null;
        }
        if (cached.expiresAt().isAfter(Instant.now())) {
            return cached.outcome();
        }

        recentSuccess.remove(key, cached);
        return null;
    }

    private void saveRecentSuccess(String key, RefreshOutcome outcome) {
        Duration grace = graceDuration == null ? Duration.ofSeconds(2) : graceDuration;
        CachedOutcome cached = new CachedOutcome(outcome, Instant.now().plus(grace));
        recentSuccess.put(key, cached);

        // 조회가 없어도 Map 항목이 남지 않도록 grace 시간이 지나면 자동으로 제거한다.
        Schedulers.parallel().schedule(
                () -> recentSuccess.remove(key, cached),
                grace.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * RT 원문 대신 비교용 지문을 생성
     * 이 값도 로그에는 전체를 남기지 않고 앞부분만 사용
     */
    private String fingerprint(String refreshToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256은 표준 JDK 알고리즘이므로 사용할 수 없다면 애플리케이션 설정 오류로 처리한다.
            throw new IllegalStateException("SHA-256 algorithm is unavailable", e);
        }
    }

    private String abbreviated(String key) {
        return key.substring(0, Math.min(12, key.length()));
    }

    // 성공 결과와 grace 만료 시각을 한 묶음으로 보관한다.
    private record CachedOutcome(RefreshOutcome outcome, Instant expiresAt) {
    }
}