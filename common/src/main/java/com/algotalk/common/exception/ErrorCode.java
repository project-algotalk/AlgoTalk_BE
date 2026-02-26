package com.algotalk.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 인터페이스
 * 각 서비스에서 Enum으로 구현
 *
 * 네임스페이스 규칙:
 *   userService      → USER_001, USER_002 ...
 *   interviewService → ITV_001, ITV_002 ...
 *   communityService → COM_001, COM_002 ...
 */
public interface ErrorCode {
    String getCode();
    String getMessage();
    HttpStatus getHttpStatus();
}
