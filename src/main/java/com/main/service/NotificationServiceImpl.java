package com.main.service;

import com.main.model.dto.JobStatusResponse;
import com.main.model.dto.NotificationDto;
import com.main.model.dto.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @Value("${app.notification.enabled:${notification.feature.enabled:true}}")
    private boolean notificationEnabled;

    @Override
    public boolean isEnabled() {
        return this.notificationEnabled;
    }

    @Override
    public void sendToUser(String username, NotificationDto notification) {
        if (!notificationEnabled) {
            log.info("[NOTIFICATION DISABLED] Skipping private notification to {}: {}", username, notification.getTitle());
            return;
        }
        log.info("Sending private notification to user: {} -> {}", username, notification.getTitle());
        // Spring STOMP converts this to destination: /user/{username}/queue/notifications
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", notification);
    }

    @Override
    public void broadcast(NotificationDto notification) {
        if (!notificationEnabled) {
            log.info("[NOTIFICATION DISABLED] Skipping broadcast: {}", notification.getTitle());
            return;
        }
        log.info("Broadcasting notification to /topic/notifications: {}", notification.getTitle());
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }

    @Override
    public void sendJobUpdate(String jobId, String targetUser, JobStatusResponse status) {
        if (!notificationEnabled) {
            log.info("[NOTIFICATION DISABLED] Skipping job update for jobId: {}", jobId);
            return;
        }
        log.info("Sending job update for jobId: {}, status: {}", jobId, status.getStatus());
        // Broadcast to specific job topic
        messagingTemplate.convertAndSend("/topic/jobs/" + jobId, status);

        // Also notify user directly if specified
        if (targetUser != null && !targetUser.trim().isEmpty()) {
            NotificationDto userNotification = NotificationDto.builder()
                    .recipient(targetUser)
                    .title("Bulk Upload Update: " + status.getStatus())
                    .message(String.format("Job %s is now %s. Processed: %d/%d (Success: %d, Failed: %d)",
                            jobId, status.getStatus(), status.getProcessedRows(), status.getTotalRows(),
                            status.getSuccessfulRows(), status.getFailedRows()))
                    .type(NotificationType.BULK_UPLOAD)
                    .metadata(Map.of(
                            "jobId", jobId,
                            "status", status.getStatus(),
                            "processedRows", status.getProcessedRows(),
                            "totalRows", status.getTotalRows()
                    ))
                    .build();
            sendToUser(targetUser, userNotification);
        }
    }
}
