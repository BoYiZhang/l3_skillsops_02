package com.boyi.skillops.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "参数校验失败"),
    USERNAME_EXISTS(40001, "用户名已存在"),
    INVALID_STATUS(40002, "当前状态不允许此操作"),
    NOT_INSTALLED(40003, "请先安装再评分"),
    ALREADY_INSTALLED(40004, "已安装过此Skill"),
    UNAUTHORIZED(401, "未登录或token已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_AUTHOR(40301, "非作者无权操作"),
    NOT_ADMIN(40302, "非管理员无权操作"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "操作冲突"),
    INTERNAL_ERROR(500, "服务端异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
