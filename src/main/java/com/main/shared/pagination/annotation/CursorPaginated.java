package com.main.shared.pagination.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CursorPaginated {
    int defaultSize() default 20;
    int maxSize()     default 100;
    String sortField() default "id";
    String sortDir()   default "ASC";
}
