package com.algotalk.userservice.service;

import java.util.List;
import java.util.Map;

public interface ILoginAttemptService {

    // 계정 잠금 여부 확인
    boolean isLocked(String loginId);

    // 여러 계정의 잠금 여부 조회
    Map<String, Boolean> getLockStatuses(List<String> loginIds);

    // 로그인 실패 횟수 증가 및 잠금 처리
    void recordFailure(String loginId);

    // 로그인 실패 횟수와 잠금 상태 초기화
    void resetAttempts(String loginId);

    // 남은 잠금 시간 조회
    long getRemainingLockSeconds(String loginId);
}
