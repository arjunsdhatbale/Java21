package com.main.shared.pagination.model;

import java.util.List;

// CursorPageResponse.java  — generic response wrapper
public record CursorPageResponse<T>(
        List<T> data,
        String nextCursor,    // null if no more pages
        String prevCursor,    // null if first page
        boolean hasNext,
        boolean hasPrev,
        int size
) {
    public static <T> CursorPageResponse<T> of(
            List<T> data, String nextCursor, String prevCursor, int requestedSize
    ) {
        return new CursorPageResponse<>(
                data,
                nextCursor,
                prevCursor,
                nextCursor != null,
                prevCursor != null,
                data.size()
        );
    }
}