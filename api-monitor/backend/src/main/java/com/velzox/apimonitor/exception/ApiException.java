package com.velzox.apimonitor.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base API Exception - Parent class for all custom exceptions
 * 
 * Provides:
 * - HTTP status code
 * - Error message
 * - Optional error code for client handling
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = null;
    }

    public ApiException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public ApiException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.errorCode = null;
    }
}
