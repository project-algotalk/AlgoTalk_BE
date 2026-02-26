package com.algotalk.common.response;

import com.algotalk.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.validation.BindingResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 통일된 에러 응답 형식
 *
 * 비즈니스 오류:
 * {
 *   "code":        "USER_001",
 *   "message":     "이메일 또는 비밀번호가 올바르지 않습니다.",
 *   "timestamp":   "2026-02-23T12:34:56"
 * }
 *
 * @Valid 실패:
 * {
 *   "code":        "VALID_001",
 *   "message":     "입력값 검증에 실패했습니다.",
 *   "timestamp":   "2026-02-23T12:34:56",
 *   "fieldErrors": [
 *     { "field": "email",    "rejectedValue": "abc", "reason": "이메일 형식이 아닙니다." },
 *     { "field": "password", "rejectedValue": "",    "reason": "비밀번호는 필수입니다." }
 *   ]
 * }
 */
@Getter
public class ErrorResponse {

    private final String code;
    private final String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    /** @Valid 실패 시에만 포함. null이면 JSON에서 생략 */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<FieldError> fieldErrors;

    private ErrorResponse(String code, String message, List<FieldError> fieldErrors) {
        this.code        = code;
        this.message     = message;
        this.timestamp   = LocalDateTime.now();
        this.fieldErrors = fieldErrors;
    }

    /** 비즈니스 예외 (BusinessException) */
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /** 비즈니스 예외 + 커스텀 메시지 */
    public static ErrorResponse of(ErrorCode errorCode, String detail) {
        return new ErrorResponse(errorCode.getCode(), detail, null);
    }

    /** 코드/메시지만 있을 때 (Validation 코드 직접 지정 등) */
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null);
    }

    /** @Valid / @Validated 실패 → 필드별 오류 목록 포함 */
    public static ErrorResponse ofValidation(BindingResult bindingResult) {
        List<FieldError> fieldErrors = bindingResult.getFieldErrors().stream()
                .map(e -> new FieldError(
                        e.getField(),
                        e.getRejectedValue() == null ? "" : e.getRejectedValue().toString(),
                        e.getDefaultMessage()))
                .collect(Collectors.toList());

        return new ErrorResponse("VALID_001", "입력값 검증에 실패했습니다.", fieldErrors);
    }

    // ── 내부 DTO ────────────────────────────────────────────────────────────

    @Getter
    public static class FieldError {
        private final String field;
        private final String rejectedValue;
        private final String reason;

        public FieldError(String field, String rejectedValue, String reason) {
            this.field         = field;
            this.rejectedValue = rejectedValue;
            this.reason        = reason;
        }
    }
}
