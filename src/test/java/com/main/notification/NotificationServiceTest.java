package com.main.notification;

import com.main.shared.bulk.JobStatusResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private NotificationDto sampleDto;

    @BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "notificationEnabled", true);
        sampleDto = NotificationDto.builder()
                .recipient("testuser")
                .title("Test Title")
                .message("Test Message")
                .type(NotificationType.INFO)
                .build();
    }

    @Test
    void testSendToUser_whenDisabled_skipsSending() {
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "notificationEnabled", false);
        assertFalse(notificationService.isEnabled());

        notificationService.sendToUser("testuser", sampleDto);
        notificationService.broadcast(sampleDto);
        notificationService.sendJobUpdate("job-1", "user", null);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void testSendToUser_routesToUserQueue() {
        notificationService.sendToUser("testuser", sampleDto);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("testuser"),
                eq("/queue/notifications"),
                eq(sampleDto)
        );
    }

    @Test
    void testBroadcast_routesToTopicNotifications() {
        notificationService.broadcast(sampleDto);

        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/notifications"),
                eq(sampleDto)
        );
    }

    @Test
    void testSendJobUpdate_broadcastsAndSendsToUser() {
        JobStatusResponse status = JobStatusResponse.builder()
                .jobId("job-123")
                .status("COMPLETED")
                .totalRows(100)
                .processedRows(100)
                .successfulRows(95)
                .failedRows(5)
                .build();

        notificationService.sendJobUpdate("job-123", "uploaderUser", status);

        // Verify broadcast to topic
        verify(messagingTemplate, times(1)).convertAndSend(
                eq("/topic/jobs/job-123"),
                eq(status)
        );

        // Verify targeted message to user
        ArgumentCaptor<NotificationDto> captor = ArgumentCaptor.forClass(NotificationDto.class);
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("uploaderUser"),
                eq("/queue/notifications"),
                captor.capture()
        );

        NotificationDto captured = captor.getValue();
        assertEquals("uploaderUser", captured.getRecipient());
        assertEquals(NotificationType.BULK_UPLOAD, captured.getType());
        assertTrue(captured.getMessage().contains("job-123"));
    }
}
