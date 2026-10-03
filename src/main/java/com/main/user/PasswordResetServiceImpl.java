package com.main.user;

import com.main.notification.NotificationDto;
import com.main.notification.NotificationService;
import com.main.notification.NotificationType;
import com.main.notification.EmailService;
import com.main.notification.SmsService;
import com.main.shared.exception.BusinessException;
import com.main.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final NotificationService notificationService;

    @Value("${app.notification.email.app-url:http://localhost:8082}")
    private String appUrl;

    @Override
    @Transactional
    public String requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(user.getEmail())
                .expiryDate(expiryDate)
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        String resetUrl = appUrl + "/api/v1/users/password-reset/confirm?token=" + token;

        // 1. Send async email with reset link and token
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token, resetUrl);

        // 2. Send async SMS if phone is present
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty()) {
            smsService.sendPasswordResetSms(user.getPhone(), token);
        }

        // 3. Send WebSocket private notification
        notificationService.sendToUser(user.getEmail(), NotificationDto.of(
                user.getEmail(),
                "Password Reset Requested",
                "A password reset request was initiated for your account. Check your email for instructions.",
                NotificationType.SECURITY
        ));

        log.info("Password reset requested for email: {}. Token generated.", email);
        return "Password reset link sent to registered email and SMS";
    }

    @Override
    @Transactional
    public void confirmPasswordReset(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid password reset token"));

        if (resetToken.isUsed()) {
            throw new BusinessException("Password reset token has already been used");
        }

        if (resetToken.isExpired()) {
            throw new BusinessException("Password reset token has expired. Please request a new one.");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", resetToken.getEmail()));

        // In a production app, this would use PasswordEncoder. encode(newPassword)
        user.setPassword(newPassword);
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        // Notify user about password change
        notificationService.sendToUser(user.getEmail(), NotificationDto.of(
                user.getEmail(),
                "Password Changed",
                "Your password has been successfully updated. If you did not make this change, please contact support immediately.",
                NotificationType.SECURITY
        ));

        log.info("Password reset completed successfully for email: {}", user.getEmail());
    }
}
