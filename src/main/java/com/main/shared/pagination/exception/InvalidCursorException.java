package com.main.shared.pagination.exception;

public class InvalidCursorException extends RuntimeException{

    public InvalidCursorException(String message){
        super(message);
    }

    public InvalidCursorException(String message, Throwable cause){
        super(message, cause);
    }
}
