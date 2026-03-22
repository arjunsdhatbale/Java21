package com.main.shared.pagination.aspect;

import com.main.shared.pagination.annotation.CursorPaginated;
import com.main.shared.pagination.codec.CursorCodec;
import com.main.shared.pagination.context.CursorContext;
import com.main.shared.pagination.exception.CursorFieldAccessException;
import com.main.shared.pagination.model.CursorPageRequest;
import com.main.shared.pagination.model.CursorPageResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.util.List;

// CursorPaginationAspect.java
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class CursorPaginationAspect {

    private final CursorCodec cursorCodec;

    @Around("@annotation(cursorPaginated)")
    public Object applyPagination(
            ProceedingJoinPoint pjp,
            CursorPaginated cursorPaginated
    ) throws Throwable {

        HttpServletRequest httpRequest = getCurrentRequest();

        // 1. Extract query params from the incoming HTTP request
        String cursor    = httpRequest.getParameter("cursor");
        String sizeParam = httpRequest.getParameter("size");
        String sortField = httpRequest.getParameter("sortField");
        String sortDir   = httpRequest.getParameter("sortDir");

        int size = parseSize(sizeParam, cursorPaginated.defaultSize(), cursorPaginated.maxSize());

        CursorPageRequest pageRequest = new CursorPageRequest(
                cursor,
                size,
                defaultIfBlank(sortField, cursorPaginated.sortField()),
                defaultIfBlank(sortDir,   cursorPaginated.sortDir())
        );

        // 2. Store in ThreadLocal — available to service/repo layers
        CursorContext.set(pageRequest);

        try {
            // 3. Invoke the actual controller method
            Object result = pjp.proceed();

            // 4. Wrap the result into CursorPageResponse
            return wrapResult(result, pageRequest);

        } finally {
            // 5. Always clean up ThreadLocal
            CursorContext.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private Object wrapResult(Object result, CursorPageRequest pageRequest) {
        List<?> items;

        if (result instanceof List<?> list) {
            items = list;
        } else if (result instanceof Page<?> page) {
            items = page.getContent();
        } else if (result instanceof ResponseEntity<?> re && re.getBody() instanceof List<?> l) {
            items = l;
        } else {
            // Not a list — return as-is (e.g. single object endpoints)
            return result;
        }

        if (items.isEmpty()) {
            return CursorPageResponse.of(items, null, pageRequest.cursor(), pageRequest.size());
        }

        // Build next cursor from last item's sort field + id
        String nextCursor = buildNextCursor(items, pageRequest.sortField());
        String prevCursor = pageRequest.isFirstPage() ? null : pageRequest.cursor();

        return CursorPageResponse.of(items, nextCursor, prevCursor, pageRequest.size());
    }

    private String buildNextCursor(List<?> items, String sortField) {
        Object lastItem = items.get(items.size() - 1);
        try {
            Object id             = getFieldValue(lastItem, "id");
            Object sortFieldValue = getFieldValue(lastItem, sortField);
            return cursorCodec.encode(sortFieldValue, id);
        } catch (Exception e) {
            log.warn("Could not build cursor from field '{}'. Falling back to id.", sortField);
            Object id = getFieldValue(lastItem, "id");
            return cursorCodec.encode(id, id);
        }
    }

    private Object getFieldValue(Object obj, String fieldName) {
        try {
            // Check getter first (e.g. getCreatedAt())
            String getter = "get" + Character.toUpperCase(fieldName.charAt(0))
                    + fieldName.substring(1);
            try {
                return obj.getClass().getMethod(getter).invoke(obj);
            } catch (NoSuchMethodException ignored) {}

            // Fall back to direct field access (records, public fields)
            Field f = findField(obj.getClass(), fieldName);
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            throw new CursorFieldAccessException(
                    "Cannot read field '" + fieldName + "' from " + obj.getClass().getSimpleName()
            );
        }
    }

    private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
        while (clazz != null) {
            try { return clazz.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { clazz = clazz.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }

    private HttpServletRequest getCurrentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
    }

    private int parseSize(String param, int defaultSize, int maxSize) {
        try {
            int parsed = Integer.parseInt(param);
            return Math.min(Math.max(parsed, 1), maxSize);
        } catch (Exception e) {
            return defaultSize;
        }
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}