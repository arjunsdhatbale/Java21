package com.main.service;

import com.main.model.dto.RowError;

import java.util.List;

public interface EmailService {

    /**
     * Send an asynchronous welcome email when a new user is created/registered,
     * including user details (first name, last name, email, mobile phone, role)
     * and confirmation that they have been added to the Java21 project.
     *
     * @param toEmail   The recipient's email address
     * @param firstName The user's first name
     * @param lastName  The user's last name
     * @param phone     The user's mobile/phone number
     * @param role      The assigned user role
     */
    void sendWelcomeEmail(String toEmail, String firstName, String lastName, String phone, String role);

    /**
     * Send an asynchronous welcome email to a newly registered user (backward-compatible overload).
     *
     * @param toEmail       The recipient's email address
     * @param recipientName The recipient's name
     */
    void sendWelcomeEmail(String toEmail, String recipientName);

    /**
     * Send an asynchronous password reset email with reset token and action URL.
     *
     * @param toEmail       The recipient's email address
     * @param recipientName The recipient's name
     * @param resetToken    The generated password reset token
     * @param resetUrl      The password reset link
     */
    void sendPasswordResetEmail(String toEmail, String recipientName, String resetToken, String resetUrl);

    /**
     * Send an asynchronous summary email when a bulk upload job completes.
     *
     * @param toEmail    The recipient's email address
     * @param jobId      The bulk upload job identifier
     * @param jobType    Type of bulk upload (e.g., "Users", "Products")
     * @param total      Total rows processed
     * @param successful Successfully inserted rows
     * @param failed     Count of failed rows
     * @param errors     Detailed list of row errors
     */
    void sendBulkUploadSummaryEmail(String toEmail, String jobId, String jobType,
                                    int total, int successful, int failed,
                                    List<RowError> errors);

    /**
     * Send a general purpose HTML email.
     *
     * @param toEmail  The recipient's email address
     * @param subject  The email subject
     * @param htmlBody The HTML body content
     */
    void sendGenericEmail(String toEmail, String subject, String htmlBody);
}
