package com.intern.doctor.common;

/**
 * 业务异常：参数问题、外部调用失败等可预期的错误
 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        this(message, 400);
    }

    public BusinessException(String message, int code) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
    }

    public int getCode() {
        return code;
    }
}
