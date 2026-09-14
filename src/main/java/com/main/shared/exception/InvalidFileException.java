package com.main.shared.exception;

import org.springframework.http.HttpStatus;

public class InvalidFileException extends BusinessException {

    public InvalidFileException(String message) {
        super(message, "INVALID_FILE", HttpStatus.BAD_REQUEST);
    }
}
