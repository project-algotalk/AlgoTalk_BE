package com.algotalk.common.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * 통일된 성공 응답 형식
 * {
 *   "status":      200,
 *   "message":     "OK",
 *   "timestamp":   "2026-02-23T12:34:56",
 *   "data":        { ... }
 * }
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드(data가 없는 경우 등)는 JSON에서 생략
public class ApiResponse<T> {

    private final int status;
    private final String message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    private final T data;

    private ApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    @JsonCreator
    private ApiResponse(
            @JsonProperty("status") int status,
            @JsonProperty("message") String message,
            @JsonProperty("timestamp") LocalDateTime timestamp,
            @JsonProperty("data") T data) {
        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.data = data;
    }

    /** 200 OK + 데이터 (기본 메시지) */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), data);
    }

    /** 200 OK + 데이터 + 커스텀 메시지 */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(HttpStatus.OK.value(), message, data);
    }

    /** 200 OK (데이터 없음) */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(HttpStatus.OK.value(), HttpStatus.OK.getReasonPhrase(), null);
    }

    /** 커스텀 상태 코드 + 커스텀 메시지 + 데이터 */
    public static <T> ApiResponse<T> of(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status.value(), message, data);
    }
}
