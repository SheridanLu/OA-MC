package com.mochu.common.exception;

import com.mochu.common.enums.ErrorCode;
import lombok.Getter;

/**
 * 业务异常 — 携带 HTTP 状态码
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        this(400, message);
    }

    /** 通过 ErrorCode 枚举构造 */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }
}
