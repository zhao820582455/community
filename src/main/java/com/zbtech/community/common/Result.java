package com.zbtech.community.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结构（对齐原 foxbook 后端）
 * { "code": 200, "message": "SUCCESS", "result": ... }
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：200 成功 / 400 失败 / 2001 鉴权 / 403 权限 */
    private Integer code;
    private String message;
    private T result;

    public Result() {
    }

    public Result(Integer code, String message, T result) {
        this.code = code;
        this.message = message;
        this.result = result;
    }

    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message, data);
    }

    /** 分页结果专用：把 PageResult 塞进 result 字段 */
    public static <T> Result<PageResult<T>> dataByPage(PageResult<T> pageResult) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), pageResult);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ErrorCode.FAIL.getCode(), message, null);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}
