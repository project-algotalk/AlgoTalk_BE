package com.algotalk.common.exception;

import lombok.Getter;

/**
 * 비즈니스 예외 최상위 클래스
 * 각 서비스에서 throw new BusinessException(UserErrorCode.LOGIN_FAIL) 형태로 사용
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}