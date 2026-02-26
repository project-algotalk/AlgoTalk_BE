package com.algotalk.common.response;

import lombok.Getter;

/**
 * 공통 API 성공 응답 래퍼
 *
 * 단건:  ApiResponse.ok(data)
 * 메시지만: ApiResponse.ok("삭제되었습니다.")
 *
 * {
 *   "success": true,
 *   "data": { ... }
 * }
 */
@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final T data;

    private ApiResponse(boolean success, T data) {
        this.success = success;
        this.data    = data;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null);
    }
}
