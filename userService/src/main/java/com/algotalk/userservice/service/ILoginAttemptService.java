package com.algotalk.userservice.service;

public interface ILoginAttemptService {

    // 계정 잠금 여부 확인
    boolean isLocked(String loginId);

    // 로그인 실패 횟수 증가 및 잠금 처리
    void recordFailure(String loginId);

    // 로그인 실패 횟수와 잠금 상태 초기화
    void resetAttempts(String loginId);

    // 남은 잠금 시간 조회
    long getRemainingLockSeconds(String loginId);
}