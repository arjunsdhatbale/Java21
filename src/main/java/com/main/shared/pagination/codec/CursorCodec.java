package com.main.shared.pagination.codec;

import com.main.shared.pagination.exception.InvalidCursorException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

// CursorCodec.java
@Component
public class CursorCodec {

    // Cursor payload: "fieldValue|id"  e.g.  "2024-01-15T10:30:00|42"
    private static final String DELIMITER = "|";

    public String encode(Object sortFieldValue, Object id) {
        String raw = String.valueOf(sortFieldValue) + DELIMITER + String.valueOf(id);
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public CursorPayload decode(String cursor) throws Throwable {
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8
            );
            String[] parts = raw.split("\\|", 2);
            if (parts.length != 2) throw new IllegalArgumentException("Bad cursor");
            return new CursorPayload(parts[0], parts[1]);
        } catch (Exception e) {
            throw new Throwable("Invalid or tampered cursor: " + cursor);
        }
    }

    public record CursorPayload(String sortFieldValue, String id) {}
}
