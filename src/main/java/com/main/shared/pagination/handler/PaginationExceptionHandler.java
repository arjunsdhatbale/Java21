package com.main.shared.pagination.handler;

import com.main.shared.pagination.exception.CursorFieldAccessException;
import com.main.shared.pagination.exception.InvalidCursorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;// PaginationExceptionHandler.java
@RestControllerAdvice
public class PaginationExceptionHandler {

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCursor(InvalidCursorException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("INVALID_CURSOR", ex.getMessage()));
    }

    @ExceptionHandler(CursorFieldAccessException.class)
    public ResponseEntity<ErrorResponse> handleFieldAccess(CursorFieldAccessException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("CURSOR_CONFIG_ERROR", ex.getMessage()));
    }

    public record ErrorResponse(String code, String message) {}
}
