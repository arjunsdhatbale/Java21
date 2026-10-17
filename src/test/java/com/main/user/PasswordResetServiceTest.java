package com.main.user;

import com.main.notification.EmailService;
import com.main.notification.NotificationDto;
import com.main.notification.NotificationService;
import com.main.notification.SmsService;
import com.main.shared.exception.BusinessException;
import com.main.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private SmsService smsService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .firstName("Arjun")
                .lastName("Dhatbale")
                .email("arjun@example.com")
                .phone("9876543210")
                .password("OldPass123")
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .build();

        ReflectionTestUtils.setField(passwordResetService, "appUrl", "http://localhost:8082");
    }

    @Test
    void testRequestPasswordReset_success() {
        when(userRepository.findByEmail("arjun@example.com")).thenReturn(Optional.of(sampleUser));

        String result = passwordResetService.requestPasswordReset("arjun@example.com");

        assertNotNull(result);
        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).sendPasswordResetEmail(eq("arjun@example.com"), eq("Arjun"), anyString(), anyString());
        verify(smsService, times(1)).sendPasswordResetSms(eq("9876543210"), anyString());
        verify(notificationService, times(1)).sendToUser(eq("arjun@example.com"), any(NotificationDto.class));
    }

    @Test
    void testRequestPasswordReset_userNotFound_throwsException() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            passwordResetService.requestPasswordReset("nonexistent@example.com");
        });

        verifyNoInteractions(tokenRepository);
    }

    @Test
    void testConfirmPasswordReset_success() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(10L)
                .token("valid-token")
                .email("arjun@example.com")
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(false)
                .build();

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(userRepository.findByEmail("arjun@example.com")).thenReturn(Optional.of(sampleUser));

        passwordResetService.confirmPasswordReset("valid-token", "NewSecretPass456");

        assertEquals("NewSecretPass456", sampleUser.getPassword());
        assertTrue(token.isUsed());
        verify(userRepository, times(1)).save(sampleUser);
        verify(tokenRepository, times(1)).save(token);
        verify(notificationService, times(1)).sendToUser(eq("arjun@example.com"), any(NotificationDto.class));
    }

    @Test
    void testConfirmPasswordReset_expiredToken_throwsBusinessException() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(10L)
                .token("expired-token")
                .email("arjun@example.com")
                .expiryDate(LocalDateTime.now().minusMinutes(5)) // Expired
                .used(false)
                .build();

        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThrows(BusinessException.class, () -> {
            passwordResetService.confirmPasswordReset("expired-token", "NewPass123");
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void testConfirmPasswordReset_alreadyUsedToken_throwsBusinessException() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(10L)
                .token("used-token")
                .email("arjun@example.com")
                .expiryDate(LocalDateTime.now().plusHours(1))
                .used(true) // Already used
                .build();

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThrows(BusinessException.class, () -> {
            passwordResetService.confirmPasswordReset("used-token", "NewPass123");
        });

        verify(userRepository, never()).save(any());
    }
}
