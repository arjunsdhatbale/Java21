package com.main.service;

import com.main.model.dto.RowError;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender javaMailSender;

    @Mock
    private MimeMessage mimeMessage;

    private EmailServiceImpl emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailServiceImpl(mailSenderProvider);
        ReflectionTestUtils.setField(emailService, "notificationFeatureEnabled", true);
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@demoapp.com");
        ReflectionTestUtils.setField(emailService, "appUrl", "http://localhost:8082");
        ReflectionTestUtils.setField(emailService, "smtpUsername", "validuser@gmail.com");
        ReflectionTestUtils.setField(emailService, "smtpPassword", "apppasswordtest123");
    }

    @Test
    void testSendEmail_whenPlaceholderCredentials_runsInMockMode() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        ReflectionTestUtils.setField(emailService, "smtpUsername", "your-email@gmail.com");
        ReflectionTestUtils.setField(emailService, "smtpPassword", "your-app-password");

        assertDoesNotThrow(() -> {
            emailService.sendWelcomeEmail("user@example.com", "Test User");
        });

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void testSendEmail_whenMasterSwitchDisabled_skipsDispatch() {
        ReflectionTestUtils.setField(emailService, "notificationFeatureEnabled", false);
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);

        assertDoesNotThrow(() -> {
            emailService.sendWelcomeEmail("user@example.com", "Test User");
        });

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void testSendWelcomeEmail_whenEmailDisabled_logsWithoutError() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        assertDoesNotThrow(() -> {
            emailService.sendWelcomeEmail("user@example.com", "Test User");
        });

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void testSendWelcomeEmail_withFullUserDetails_whenEmailEnabled_sendsMimeMessage() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> {
            emailService.sendWelcomeEmail("john@example.com", "John", "Doe", "9876543210", "ADMIN");
        });

        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendWelcomeEmail_withFullUserDetails_whenEmailDisabled_logsWithoutError() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        assertDoesNotThrow(() -> {
            emailService.sendWelcomeEmail("john@example.com", "John", "Doe", "9876543210", "ADMIN");
        });

        verifyNoInteractions(javaMailSender);
    }

    @Test
    void testSendPasswordResetEmail_whenEmailEnabled_sendsMimeMessage() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> {
            emailService.sendPasswordResetEmail("user@example.com", "Test User", "token-123", "http://localhost:8082/reset");
        });

        verify(javaMailSender, times(1)).send(mimeMessage);
    }

    @Test
    void testSendBulkUploadSummaryEmail_withErrors_executesCleanly() {
        ReflectionTestUtils.setField(emailService, "emailEnabled", false);

        List<RowError> errors = List.of(
                RowError.builder().rowNumber(2).email("dup@example.com").reason("Email already exists").build(),
                RowError.builder().rowNumber(5).email("inv@domain").reason("Invalid email format").build()
        );

        assertDoesNotThrow(() -> {
            emailService.sendBulkUploadSummaryEmail("admin@demoapp.com", "job-999", "Users", 100, 98, 2, errors);
        });
    }
}
