package com.meobeo.truyen.exception;

/**
 * Exception được throw khi user chưa đăng nhập
 * hoặc không thể xác thực được user hiện tại
 */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }

    public UnauthenticatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
