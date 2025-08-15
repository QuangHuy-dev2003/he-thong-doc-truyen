package com.meobeo.truyen.exception;

public class PackageNotFoundException extends RuntimeException {

    public PackageNotFoundException(String message) {
        super(message);
    }

    public PackageNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
