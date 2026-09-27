package com.main.service;

public interface SmsService {

    /**
     * Send an asynchronous SMS text message.
     *
     * @param phoneNumber Recipient's phone number
     * @param message     SMS message text
     */
    void sendSms(String phoneNumber, String message);

    /**
     * Send password reset code/link via SMS.
     *
     * @param phoneNumber Recipient's phone number
     * @param resetToken  Password reset token
     */
    void sendPasswordResetSms(String phoneNumber, String resetToken);
}
