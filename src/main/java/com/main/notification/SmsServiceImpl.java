package com.main.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

    @Value("${app.notification.enabled:${notification.feature.enabled:true}}")
    private boolean notificationEnabled;

    @Value("${app.notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${app.notification.sms.provider:mock}")
    private String smsProvider;

    @Async("notificationExecutor")
    @Override
    public void sendSms(String phoneNumber, String message) {
        if (!notificationEnabled) {
            log.info("[NOTIFICATION DISABLED] Master switch is disabled. Skipping SMS to: {}", phoneNumber);
            return;
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("Cannot send SMS: Phone number is missing or empty.");
            return;
        }

        if (smsEnabled) {
            // Hook for real SMS provider (Twilio, AWS SNS, Infobip, etc.)
            log.info("Sending SMS via [{}] to [{}]: {}", smsProvider, phoneNumber, message);
        } else {
            log.info("""
                \n==================== [ASYNC SMS DISPATCH PREVIEW] ====================
                To: {}
                Provider: {} (MOCK / DEMO MODE)
                Message: {}
                ======================================================================
                """, phoneNumber, smsProvider, message);
        }
    }

    @Async("notificationExecutor")
    @Override
    public void sendPasswordResetSms(String phoneNumber, String resetToken) {
        String message = "Demo App: Your password reset token is: " + resetToken + ". Valid for 60 minutes.";
        sendSms(phoneNumber, message);
    }
}
