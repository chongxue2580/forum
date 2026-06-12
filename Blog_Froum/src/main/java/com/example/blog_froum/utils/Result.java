package com.example.blog_froum.utils;

import lombok.Data;

/**
 * 统一响应结果封装类
 */
@Data
public class Result<T> {
    private boolean success;
    private String message;
    private T data;
    private int code;

    public Result() {}

    public Result(boolean success, String message, T data, int code) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(true, "操作成功", null, 200);
    }

    /**
     * 成功响应带数据
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, "操作成功", data, 200);
    }

    /**
     * 成功响应带消息和数据
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, message, data, 200);
    }

    /**
     * 成功响应带消息（无数据返回，适用于Void类型）
     */
    public static Result<Void> success(String message) {
        return new Result<>(true, message, null, 200);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null, 500);
    }

    /**
     * 失败响应带状态码
     */
    public static <T> Result<T> error(String message, int code) {
        return new Result<>(false, message, null, code);
    }

    /**
     * 失败响应带数据
     */
    public static <T> Result<T> error(String message, T data) {
        return new Result<>(false, message, data, 500);
    }
}
