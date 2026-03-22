package com.main.shared.pagination.model;
// CursorPageRequest.java  — injected into ThreadLocal, readable anywhere
public record CursorPageRequest(
        String cursor,       // opaque, base64-encoded last-seen ID + sort field
        int size,            // page size
        String sortField,    // e.g. "createdAt"
        String sortDir       // "ASC" or "DESC"
) {
    public static CursorPageRequest defaults() {
        return new CursorPageRequest(null, 20, "id", "ASC");
    }

    public boolean isFirstPage() { return cursor == null || cursor.isBlank(); }
}