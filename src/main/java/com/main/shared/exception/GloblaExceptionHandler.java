package com.main.shared.exception;

import com.main.shared.pagination.exception.CursorFieldAccessException;
import com.main.shared.pagination.exception.InvalidCursorException;
import com.main.shared.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GloblaExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {

        ProblemDetail problem = ProblemDetail.forStatus(ex.getStatus());
        problem.setTitle("Business Exception");
        problem.setDetail(ex.getMessage());
        problem.setProperty("errorCode", ex.getErrorCode());

        return problem;
    }

    @ExceptionHandler(InvalidCursorException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidcursor(InvalidCursorException ex){
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("INVALID_CURSOR", ex.getMessage()));
    }

    @ExceptionHandler(CursorFieldAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleCursorFieldAccess(CursorFieldAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("CURSOR_CONFIG_ERROR", ex.getMessage()));
    }
}
