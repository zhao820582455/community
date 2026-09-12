package com.zbtech.community.common;

import lombok.Getter;

/**
 * 状态码约定（对齐原 foxbook）
 */
@Getter
public enum ErrorCode {

    SUCCESS(200, "SUCCESS"),
    FAIL(400, "操作失败"),
    UNAUTHORIZED(2001, "请先登录"),
    FORBIDDEN(403, "没有权限"),
    PARAM_ERROR(422, "参数错误"),
    NOT_FOUND(404, "资源不存在");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
