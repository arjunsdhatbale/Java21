package com.main.shared.exception;


import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException{
    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, HttpStatus status) {
        super(message, status);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(
                resourceName + " not found with " + fieldName + " : " + fieldValue,
                "RESOURCE_NOT_FOUND",
                HttpStatus.NOT_FOUND
        );
    }
}
