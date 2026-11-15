package com.youkong.common;

import lombok.Data;

@Data
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = ErrorCode.OK.getCode();
        r.message = ErrorCode.OK.getMessage();
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = errorCode.getCode();
        r.message = errorCode.getMessage();
        return r;
    }
}
