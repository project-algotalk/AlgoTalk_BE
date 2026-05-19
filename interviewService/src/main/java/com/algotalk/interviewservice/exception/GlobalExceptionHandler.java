package com.algotalk.interviewservice.exception;

import com.algotalk.common.exception.BusinessException;
import com.algotalk.common.response.ErrorResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static com.algotalk.interviewservice.exception.InterviewErrorCode.INTERNAL_ERROR;

/**
 * userService 전역 예외 처리기
 *
 * 처리 우선순위:
 *  1. BusinessException → 의도한 비즈니스 오류 (4xx)
 *  2. MethodArgumentNotValidException → @Valid 실패 (필드별 오류 목록 반환)
 *  3. Exception → 예상치 못한 서버 오류 (500)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * throw new BusinessException(UserErrorCode.LOGIN_FAIL)
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("[BusinessException] code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getHttpStatus())
                .body(ErrorResponse.of(e.getErrorCode()));
    }

    /**
     * @Valid / @Validated 실패 처리
     * 모든 필드 오류를 fieldErrors 배열로 반환
     *
     * {
     *   "code": "VALID_001",
     *   "message": "입력값 검증에 실패했습니다.",
     *   "timestamp": "...",
     *   "fieldErrors": [
     *     { "field": "email", "rejectedValue": "abc", "reason": "이메일 형식이 아닙니다." }
     *   ]
     * }
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        ErrorResponse response = ErrorResponse.ofValidation(e.getBindingResult());
        log.warn("[ValidationException] fieldErrors={}", response.getFieldErrors());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 예상치 못한 서버 오류 처리
     * 상세 내용은 로그에만 남기고 클라이언트에는 일반 메시지 반환
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[UnhandledException] {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(INTERNAL_ERROR));
    }
}
