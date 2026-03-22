package com.main.shared.pagination.context;

import com.main.shared.pagination.model.CursorPageRequest;

// CursorContext.java  — ThreadLocal holder, aspect writes, repo reads
public final class CursorContext {
    private static final ThreadLocal<CursorPageRequest> HOLDER = new ThreadLocal<>();

    public static void set(CursorPageRequest req) { HOLDER.set(req); }
    public static CursorPageRequest get() {
        CursorPageRequest req = HOLDER.get();
        return req != null ? req : CursorPageRequest.defaults();
    }
    public static void clear() { HOLDER.remove(); }
}