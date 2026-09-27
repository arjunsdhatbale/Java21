package com.main.service;

import com.main.model.dto.JobStatusResponse;
import com.main.model.dto.NotificationDto;

public interface NotificationService {

    /**
     * Send a private, user-targeted notification via WebSocket STOMP.
     * Delivered to: /user/{username}/queue/notifications
     *
     * @param username     The recipient's username or user identifier
     * @param notification The notification payload
     */
    void sendToUser(String username, NotificationDto notification);

    /**
     * Broadcast a notification to all connected clients.
     * Delivered to: /topic/notifications
     *
     * @param notification The notification payload
     */
    void broadcast(NotificationDto notification);

    /**
     * Send bulk upload job progress / completion update over WebSocket.
     * Broadcasts to /topic/jobs/{jobId} and optionally to target user.
     *
     * @param jobId      The bulk upload job id
     * @param targetUser The user who initiated the job (optional)
     * @param status     The current job status payload
     */
    void sendJobUpdate(String jobId, String targetUser, JobStatusResponse status);

    /**
     * Check whether notifications are enabled in application configuration.
     *
     * @return true if enabled, false otherwise
     */
    boolean isEnabled();
}
