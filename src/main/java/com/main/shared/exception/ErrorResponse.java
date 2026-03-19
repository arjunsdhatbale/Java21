package com.main.shared.exception;

import lombok.Data;


@Data
public class ErrorResponse {
    private String errorCode;
    private String message;

    public ErrorResponse(String errorCode, String message) {
        this.errorCode = errorCode;
        this.message = message;
    }
    public String getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }
}
