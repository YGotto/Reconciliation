package com.reconciliation.common;

import lombok.Data;

@Data
public class ApiResult<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> ApiResult<T> success(T object) {
        ApiResult<T> result = new ApiResult<>();
        result.code = 1;
        result.data = object;
        return result;
    }

    public static <T> ApiResult<T> error(String msg) {
        ApiResult<T> result = new ApiResult<>();
        result.code = 0;
        result.msg = msg;
        return result;
    }
}