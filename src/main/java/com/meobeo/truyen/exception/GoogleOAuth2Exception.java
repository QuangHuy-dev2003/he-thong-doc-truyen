package com.meobeo.truyen.exception;

public class GoogleOAuth2Exception extends RuntimeException {

    public GoogleOAuth2Exception(String message) {
        super(message);
    }

    public GoogleOAuth2Exception(String message, Throwable cause) {
        super(message, cause);
    }
}