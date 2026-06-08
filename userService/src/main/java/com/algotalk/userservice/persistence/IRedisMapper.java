package com.algotalk.userservice.persistence;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface IRedisMapper {

    // 지정한 만료 시간과 함께 문자열 값을 저장
    void setValue(String key, String value, long timeout, TimeUnit unit);

    // key에 저장된 문자열을 조회하며, 존재하지 않으면 null을 반환
    String getValue(String key);

    // key와 연결된 값을 삭제
    void delete(String key);

    /**
     * Redis Set에 member 추가
     *
     * @param key Redis Set key
     * @param member 추가할 Set member
     */
    void addSetMember(String key, String member);

    /**
     * Redis Set에서 member 제거
     *
     * @param key Redis Set key
     * @param member 제거할 Set member
     */
    void removeSetMember(String key, String member);

    /**
     * Redis Set의 전체 member 조회
     *
     * @param key Redis Set key
     * @return Set에 저장된 member 목록
     */
    Set<String> getSetMembers(String key);

    /**
     * 현재 저장값이 expectedValue와 일치할 때에만 newValue로 교체하고 TTL을 갱신
     *
     * 비교와 저장을 별도 명령으로 실행하면 두 요청이 동시에 통과할 수 있으므로,
     * 구현체는 Redis Lua script 한 번으로 전체 과정을 원자적으로 실행해야 됨
     */
    CompareAndSetResult compareAndSet(
            String key,
            String expectedValue,
            String newValue,
            long timeout,
            TimeUnit unit
    );

    /**
     * Redis CAS 실행 결과를 서비스 계층이 이해하기 쉬운 값으로 표현
     *
     */
    enum CompareAndSetResult {
        // 기존 값이 일치하여 새 값과 TTL 저장까지 완료됨
        UPDATED,

        // 비교할 Redis key 자체가 존재하지 않음
        NOT_FOUND,

        // key는 존재하지만 저장값이 요청의 기존 RT와 다름
        MISMATCH
    }
}
