package com.meobeo.truyen.exception;

public class AccountAlreadyActivatedException extends RuntimeException {

    public AccountAlreadyActivatedException(String message) {
        super(message);
    }

    public AccountAlreadyActivatedException(String message, Throwable cause) {
        super(message, cause);
    }
}