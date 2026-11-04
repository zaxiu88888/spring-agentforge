package com.agentforge.model.common;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 统一 REST 返回结构：{ code, message, data }。
 * 业务错误 HTTP 状态码保持 200，通过 code 区分（见 docs/api-contract.md §0）。
 */
@Getter
public class ApiResponse<T> {

    private final int code;
    private final String message;
    private final T data;

    private ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ApiCode.SUCCESS.getCode(), ApiCode.SUCCESS.getMessage(), data);
    }

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(ApiCode.ERROR.getCode(), message, null);
    }

    @Getter
    public enum ApiCode {
        SUCCESS(200, "success"),
        ERROR(500, "error");

        private final int code;
        @JsonValue
        private final String message;

        ApiCode(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
