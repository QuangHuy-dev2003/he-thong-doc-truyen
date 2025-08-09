package com.meobeo.truyen.exception;

/**
 * Exception generic cho các lỗi nghiệp vụ với message tùy chỉnh
 * Có thể dùng cho mọi trường hợp cần trả về lỗi 400 với message cụ thể
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
