package com.example.project.exception;

import lombok.Getter;

/**
 * Custom exception class for handling business-related errors.
 * This exception is used to represent specific business logic violations
 * and includes an error code for identifying the type of error.
 */

@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code) {
        super(code);
        this.code = code;
    }
}