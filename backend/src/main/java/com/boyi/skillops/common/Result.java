package com.boyi.skillops.common;

import com.boyi.skillops.enums.ErrorCode;
import lombok.Data;

@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(ErrorCode ec) {
        Result<T> r = new Result<>();
        r.code = ec.getCode();
        r.message = ec.getMessage();
        return r;
    }

    public static <T> Result<T> error(ErrorCode ec, String msg) {
        Result<T> r = new Result<>();
        r.code = ec.getCode();
        r.message = msg;
        return r;
    }
}
