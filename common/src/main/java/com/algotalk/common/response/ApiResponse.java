package com.algotalk.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 공통 API 성공 응답 래퍼
 *
 * 단건:  ApiResponse.ok(data)
 * 빈 응답: ApiResponse.ok()
 *
 * {
 *   "success": true,
 *   "data": { ... }      ← data가 null이면 JSON에서 생략
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null);
    }
}
