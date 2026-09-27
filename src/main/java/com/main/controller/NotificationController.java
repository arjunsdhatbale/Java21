package com.main.controller;

import com.main.model.dto.NotificationDto;
import com.main.model.dto.NotificationType;
import com.main.service.EmailService;
import com.main.service.NotificationService;
import com.main.service.SmsService;
import com.main.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final SmsService smsService;

    // ═══════════════════════════════════════════════
    //        WEBSOCKET STOMP MESSAGE MAPPINGS
    // ═══════════════════════════════════════════════

    /**
     * Broadcast to /topic/notification (existing compatibility)
     */
    @MessageMapping("/send-message")
    @SendTo("/topic/notification")
    public String sendMessage(String message) {
        logger.info("Request received to send notification: {}", message);
        return message;
    }

    /**
     * Broadcast chat message to /topic/notification (existing compatibility)
     */
    @MessageMapping("/chat")
    @SendTo("/topic/notification")
    public String sendChatMessage(String message) {
        logger.info("Request received for chat message: {}", message);
        return message;
    }

    /**
     * User-targeted message echo using @SendToUser.
     * Delivered to: /user/queue/notifications of the sending user.
     */
    @MessageMapping("/private-notification")
    @SendToUser("/queue/notifications")
    public NotificationDto handlePrivateNotification(@Payload String message, Principal principal) {
        String username = principal != null ? principal.getName() : "anonymous";
        logger.info("Received private notification request from user [{}]: {}", username, message);
        return NotificationDto.builder()
                .recipient(username)
                .title("Private Response")
                .message("Echo for " + username + ": " + message)
                .type(NotificationType.INFO)
                .build();
    }

    /**
     * Peer-to-peer or client-initiated message to a specific user.
     */
    @MessageMapping("/send-to-user")
    public void sendToSpecificUser(@Payload NotificationDto notification, Principal principal) {
        String sender = principal != null ? principal.getName() : "system";
        logger.info("User [{}] sending notification to recipient [{}]: {}",
                sender, notification.getRecipient(), notification.getTitle());
        notificationService.sendToUser(notification.getRecipient(), notification);
    }

    // ═══════════════════════════════════════════════
    //            REST API ENDPOINTS
    // ═══════════════════════════════════════════════

    /**
     * Send private notification to a connected user via REST
     */
    @PostMapping("/api/v1/notifications/user/{username}")
    public ResponseEntity<ApiResponse<NotificationDto>> sendNotificationToUser(
            @PathVariable String username,
            @RequestBody NotificationDto request) {
        logger.info("REST request to send notification to user: {}", username);
        NotificationDto notification = NotificationDto.builder()
                .recipient(username)
                .title(request.getTitle() != null ? request.getTitle() : "Notification")
                .message(request.getMessage())
                .type(request.getType() != null ? request.getType() : NotificationType.INFO)
                .metadata(request.getMetadata())
                .build();

        notificationService.sendToUser(username, notification);
        return ResponseEntity.ok(ApiResponse.success("Notification sent to user " + username, notification));
    }

    /**
     * Broadcast notification to all connected clients
     */
    @PostMapping("/api/v1/notifications/broadcast")
    public ResponseEntity<ApiResponse<NotificationDto>> broadcastNotification(
            @RequestBody NotificationDto request) {
        logger.info("REST request to broadcast notification: {}", request.getTitle());
        NotificationDto notification = NotificationDto.builder()
                .title(request.getTitle() != null ? request.getTitle() : "Announcement")
                .message(request.getMessage())
                .type(request.getType() != null ? request.getType() : NotificationType.INFO)
                .metadata(request.getMetadata())
                .build();

        notificationService.broadcast(notification);
        return ResponseEntity.ok(ApiResponse.success("Broadcast sent successfully", notification));
    }

    /**
     * Test sending email asynchronously
     */
    @PostMapping("/api/v1/notifications/test-email")
    public ResponseEntity<ApiResponse<String>> testEmail(
            @RequestParam String toEmail,
            @RequestParam(defaultValue = "Arjun") String name,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false, defaultValue = "USER") String role) {
        logger.info("REST request to send test welcome email to: {}", toEmail);
        if (lastName != null && !lastName.isBlank()) {
            emailService.sendWelcomeEmail(toEmail, name, lastName, phone, role);
        } else {
            emailService.sendWelcomeEmail(toEmail, name);
        }
        return ResponseEntity.ok(ApiResponse.success("Welcome email queued for delivery to " + toEmail, toEmail));
    }

    /**
     * Test sending SMS asynchronously
     */
    @PostMapping("/api/v1/notifications/test-sms")
    public ResponseEntity<ApiResponse<String>> testSms(
            @RequestParam String phone,
            @RequestParam(defaultValue = "Hello from Demo App!") String message) {
        logger.info("REST request to send test SMS to: {}", phone);
        smsService.sendSms(phone, message);
        return ResponseEntity.ok(ApiResponse.success("SMS queued for delivery to " + phone, phone));
    }

    /**
     * Check if the notification feature is active or disabled in application configuration.
     */
    @GetMapping("/api/v1/notifications/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotificationStatus() {
        boolean enabled = notificationService.isEnabled();
        Map<String, Object> status = Map.of(
                "enabled", enabled,
                "status", enabled ? "ACTIVE" : "INACTIVE",
                "message", enabled ? "Notification feature is ACTIVE in application configuration"
                                   : "Notification feature is INACTIVE (disabled in application.properties/yml)"
        );
        return ResponseEntity.ok(ApiResponse.success("Notification feature status fetched", status));
    }
}

