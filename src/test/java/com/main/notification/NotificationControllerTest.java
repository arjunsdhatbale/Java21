package com.main.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @InjectMocks
    private NotificationController notificationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController).build();
    }

    @Test
    void testSendNotificationToUser() throws Exception {
        String json = """
            {
              "title": "Welcome Back",
              "message": "You have a new message",
              "type": "INFO"
            }
            """;

        mockMvc.perform(post("/api/v1/notifications/user/arjun")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recipient").value("arjun"))
                .andExpect(jsonPath("$.data.title").value("Welcome Back"));

        verify(notificationService, times(1)).sendToUser(eq("arjun"), any(NotificationDto.class));
    }

    @Test
    void testBroadcastNotification() throws Exception {
        String json = """
            {
              "title": "System Alert",
              "message": "Scheduled maintenance in 10 minutes",
              "type": "WARNING"
            }
            """;

        mockMvc.perform(post("/api/v1/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("System Alert"));

        verify(notificationService, times(1)).broadcast(any(NotificationDto.class));
    }

    @Test
    void testTestEmailEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/test-email")
                        .param("toEmail", "test@example.com")
                        .param("name", "Arjun"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(emailService, times(1)).sendWelcomeEmail("test@example.com", "Arjun");
    }

    @Test
    void testTestSmsEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/notifications/test-sms")
                        .param("phone", "9876543210")
                        .param("message", "Hello!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(smsService, times(1)).sendSms("9876543210", "Hello!");
    }

    @Test
    void testGetNotificationStatusEndpoint() throws Exception {
        when(notificationService.isEnabled()).thenReturn(true);

        mockMvc.perform(get("/api/v1/notifications/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }
}
