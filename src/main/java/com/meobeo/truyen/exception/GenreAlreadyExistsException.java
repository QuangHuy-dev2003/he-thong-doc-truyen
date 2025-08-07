package com.meobeo.truyen.exception;

public class GenreAlreadyExistsException extends RuntimeException {
    
    public GenreAlreadyExistsException(String message) {
        super(message);
    }
    
    public GenreAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
} 