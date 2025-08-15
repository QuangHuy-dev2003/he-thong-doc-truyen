package com.meobeo.truyen.exception;

public class PackageInactiveException extends RuntimeException {

    public PackageInactiveException(String message) {
        super(message);
    }

    public PackageInactiveException(String message, Throwable cause) {
        super(message, cause);
    }
}
