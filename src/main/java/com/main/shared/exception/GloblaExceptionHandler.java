package com.main.shared.exception;

import org.springframework.http.ProblemDetail;
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
}
