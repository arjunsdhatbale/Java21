package com.main.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

    @Builder.Default
    private String id = UUID.randomUUID().toString();

    private String recipient;

    private String title;

    private String message;

    @Builder.Default
    private NotificationType type = NotificationType.INFO;

    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();

    private Map<String, Object> metadata;

    public static NotificationDto of(String recipient, String title, String message, NotificationType type) {
        return NotificationDto.builder()
                .id(UUID.randomUUID().toString())
                .recipient(recipient)
                .title(title)
                .message(message)
                .type(type)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
