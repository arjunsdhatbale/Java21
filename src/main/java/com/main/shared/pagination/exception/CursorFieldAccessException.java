package com.main.shared.pagination.exception;

public class CursorFieldAccessException extends RuntimeException{

    public CursorFieldAccessException(String message){
        super(message);
    }

    public CursorFieldAccessException(String message, Throwable cause){
        super(message, cause);
    }
}
