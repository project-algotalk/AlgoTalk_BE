package com.algotalk.apigateway.exception;

import com.algotalk.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum GatewayErrorCode implements ErrorCode {
    // 토큰
    TOKEN_EXPIRED           ("AUTH_001", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID           ("AUTH_002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED), // 토큰 형식 자체가 문제가 있을 때(위변조, 구조 오류 등)
    TOKEN_NOT_FOUND ("AUTH_003", "토큰이 존재하지 않습니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_MISMATCH          ("AUTH_004", "토큰이 일치하지 않습니다.", HttpStatus.UNAUTHORIZED), // DB에 저장된 Refresh Token과 비교해서 일치하지 않을 때
    SESSION_REVOKED         ("AUTH_005", "로그인 세션이 만료되었거나 로그아웃되었습니다.", HttpStatus.UNAUTHORIZED),

    // 서버
    INTERNAL_ERROR          ("USER_999", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
