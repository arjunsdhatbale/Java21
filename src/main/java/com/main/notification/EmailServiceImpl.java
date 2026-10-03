package com.main.notification;

import com.main.shared.bulk.RowError;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.notification.enabled:${notification.feature.enabled:true}}")
    private boolean notificationFeatureEnabled;

    @Value("${app.notification.email.enabled:${app.notification.enabled:${notification.feature.enabled:true}}}")
    private boolean emailEnabled;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    @Value("${app.notification.email.from:${spring.mail.username:noreply@demoapp.com}}")
    private String fromEmail;

    @Value("${app.notification.email.app-url:http://localhost:8082}")
    private String appUrl;

    public EmailServiceImpl(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    @Async("notificationExecutor")
    @Override
    public void sendWelcomeEmail(String toEmail, String firstName, String lastName, String phone, String role) {
        String safeFirst = firstName != null ? firstName.trim() : "";
        String safeLast = lastName != null ? lastName.trim() : "";
        String safeFullName = (safeFirst + " " + safeLast).trim();
        if (safeFullName.isEmpty()) {
            safeFullName = "User";
        }
        String safePhone = (phone != null && !phone.isBlank()) ? phone.trim() : "Not Provided";
        String safeRole = (role != null && !role.isBlank()) ? role.trim() : "USER";

        String subject = "You have been added to my Java21 project - Welcome, " + safeFullName + "!";
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; line-height: 1.6; color: #1e293b; margin: 0; padding: 24px; background-color: #f1f5f9; }
                .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; box-shadow: 0 10px 25px rgba(0,0,0,0.08); border: 1px solid #e2e8f0; }
                .header { background: linear-gradient(135deg, #4f46e5 0%%, #7c3aed 100%%); padding: 36px 30px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 26px; font-weight: 700; letter-spacing: -0.5px; }
                .header p { margin: 8px 0 0 0; opacity: 0.92; font-size: 14px; }
                .content { padding: 32px 30px; }
                .greeting { font-size: 19px; font-weight: 700; margin-bottom: 14px; color: #0f172a; }
                .announcement-box { background: #eef2ff; border-left: 4px solid #6366f1; border-radius: 6px; padding: 14px 18px; margin: 18px 0; }
                .announcement-box h3 { margin: 0 0 4px 0; color: #3730a3; font-size: 16px; font-weight: 700; }
                .announcement-box p { margin: 0; color: #4338ca; font-size: 14px; }
                .section-title { font-size: 13px; font-weight: 700; text-transform: uppercase; letter-spacing: 0.8px; color: #64748b; margin: 22px 0 10px 0; }
                .details-table { width: 100%%; border-collapse: separate; border-spacing: 0; border: 1px solid #e2e8f0; border-radius: 8px; overflow: hidden; margin-bottom: 22px; }
                .details-table tr:not(:last-child) td { border-bottom: 1px solid #f1f5f9; }
                .details-table td { padding: 11px 16px; font-size: 14px; }
                .details-label { background: #f8fafc; font-weight: 600; color: #475569; width: 38%%; }
                .details-value { background: #ffffff; color: #0f172a; font-weight: 500; }
                .role-pill { display: inline-block; padding: 2px 10px; border-radius: 9999px; font-size: 12px; font-weight: 700; background: #e0e7ff; color: #4338ca; text-transform: uppercase; }
                .features { background: #f8fafc; border-radius: 8px; padding: 18px 20px; margin: 20px 0; border: 1px solid #e2e8f0; }
                .features ul { margin: 8px 0 0 0; padding-left: 20px; }
                .features li { margin-bottom: 6px; color: #334155; font-size: 13px; }
                .button-container { text-align: center; margin: 26px 0; }
                .button { display: inline-block; padding: 12px 30px; background: #4f46e5; color: #ffffff !important; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 14px; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>🎉 Welcome to Java21 Project!</h1>
                  <p>Account Registration Confirmation</p>
                </div>
                <div class="content">
                  <div class="greeting">Hello %s,</div>
                  
                  <div class="announcement-box">
                    <h3>You have been added to my Java21 project</h3>
                    <p>Your user profile has been created successfully. Welcome aboard!</p>
                  </div>

                  <div class="section-title">Your Registered Information</div>
                  <table class="details-table">
                    <tr>
                      <td class="details-label">First Name</td>
                      <td class="details-value">%s</td>
                    </tr>
                    <tr>
                      <td class="details-label">Last Name</td>
                      <td class="details-value">%s</td>
                    </tr>
                    <tr>
                      <td class="details-label">Email Address</td>
                      <td class="details-value"><strong>%s</strong></td>
                    </tr>
                    <tr>
                      <td class="details-label">Mobile Number</td>
                      <td class="details-value">%s</td>
                    </tr>
                    <tr>
                      <td class="details-label">Assigned Role</td>
                      <td class="details-value"><span class="role-pill">%s</span></td>
                    </tr>
                  </table>

                  <div class="features">
                    <strong style="color: #1e293b; font-size: 14px;">Next steps in Java21:</strong>
                    <ul>
                      <li>Experience real-time notifications via WebSocket STOMP</li>
                      <li>Explore product catalogs and search APIs with keyset pagination</li>
                      <li>Process high-speed bulk Excel imports with instant job feedback</li>
                    </ul>
                  </div>

                  <div class="button-container">
                    <a href="%s" class="button">Go to Dashboard</a>
                  </div>

                  <p style="font-size: 13px; color: #64748b; margin-top: 20px;">
                    If you have any questions, feel free to reply directly to this email.
                  </p>
                </div>
                <div class="footer">
                  © 2026 Java21 Project. All rights reserved.<br>
                  This is an automated system notification.
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                escapeHtml(safeFullName),
                escapeHtml(safeFirst),
                escapeHtml(safeLast),
                escapeHtml(toEmail),
                escapeHtml(safePhone),
                escapeHtml(safeRole),
                appUrl
            );

        sendGenericEmail(toEmail, subject, html);
    }

    @Async("notificationExecutor")
    @Override
    public void sendWelcomeEmail(String toEmail, String recipientName) {
        sendWelcomeEmail(toEmail, recipientName, "", "Not Provided", "USER");
    }

    @Async("notificationExecutor")
    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetToken, String resetUrl) {
        String subject = "Password Reset Request for Demo App";
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f4f7f6; }
                .container { max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                .header { background: linear-gradient(135deg, #e11d48, #be123c); padding: 36px 30px; text-align: center; color: #ffffff; }
                .header h1 { margin: 0; font-size: 24px; font-weight: 700; }
                .content { padding: 32px 30px; }
                .token-box { background: #fef2f2; border: 1px dashed #f87171; border-radius: 8px; padding: 14px; text-align: center; font-family: monospace; font-size: 18px; font-weight: bold; color: #991b1b; letter-spacing: 2px; margin: 20px 0; }
                .button { display: inline-block; padding: 12px 28px; background: #e11d48; color: #ffffff !important; text-decoration: none; border-radius: 8px; font-weight: 600; margin: 20px 0; }
                .alert { background: #fffbeb; border-left: 4px solid #f59e0b; padding: 12px 16px; margin: 18px 0; font-size: 13px; color: #92400e; }
                .footer { background: #f8fafc; padding: 20px; text-align: center; font-size: 13px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h1>🔒 Password Reset Request</h1>
                </div>
                <div class="content">
                  <p>Hello <strong>%s</strong>,</p>
                  <p>We received a request to reset your password. Use the token below or click the button to set a new password:</p>
                  
                  <div class="token-box">%s</div>
                  
                  <p style="text-align: center;">
                    <a href="%s" class="button">Reset Password</a>
                  </p>

                  <div class="alert">
                    ⚠️ <strong>Security Notice:</strong> This link and token will expire in <strong>60 minutes</strong>. If you did not make this request, you can safely ignore this email.
                  </div>
                </div>
                <div class="footer">
                  © 2026 Demo Application. All rights reserved.
                </div>
              </div>
            </body>
            </html>
            """.formatted(escapeHtml(recipientName), resetToken, resetUrl);

        sendGenericEmail(toEmail, subject, html);
    }

    @Async("notificationExecutor")
    @Override
    public void sendBulkUploadSummaryEmail(String toEmail, String jobId, String jobType,
                                          int total, int successful, int failed,
                                          List<RowError> errors) {
        String subject = String.format("Bulk Upload Completed [%s] - Job %s", jobType, jobId.substring(0, Math.min(8, jobId.length())));

        StringBuilder errorRowsHtml = new StringBuilder();
        if (errors != null && !errors.isEmpty()) {
            errorRowsHtml.append("""
                <h3 style="margin-top: 24px; color: #b91c1c;">Failed Rows Details (Sample)</h3>
                <table style="width: 100%; border-collapse: collapse; font-size: 13px; margin-top: 8px;">
                  <thead>
                    <tr style="background: #fee2e2; color: #991b1b; text-align: left;">
                      <th style="padding: 8px; border: 1px solid #fca5a5;">Row #</th>
                      <th style="padding: 8px; border: 1px solid #fca5a5;">Identifier</th>
                      <th style="padding: 8px; border: 1px solid #fca5a5;">Reason</th>
                    </tr>
                  </thead>
                  <tbody>
            """);

            int maxDisplay = Math.min(errors.size(), 10);
            for (int i = 0; i < maxDisplay; i++) {
                RowError err = errors.get(i);
                String identifier = err.getEmail() != null ? err.getEmail() : (err.getIdentifier() != null ? err.getIdentifier() : "-");
                errorRowsHtml.append(String.format("""
                    <tr>
                      <td style="padding: 8px; border: 1px solid #e2e8f0; font-weight: bold;">%d</td>
                      <td style="padding: 8px; border: 1px solid #e2e8f0;">%s</td>
                      <td style="padding: 8px; border: 1px solid #e2e8f0; color: #dc2626;">%s</td>
                    </tr>
                """, err.getRowNumber(), escapeHtml(identifier), escapeHtml(err.getReason())));
            }

            if (errors.size() > 10) {
                errorRowsHtml.append(String.format("""
                    <tr>
                      <td colspan="3" style="padding: 8px; text-align: center; color: #64748b; font-style: italic;">
                        ... and %d more errors. Please check the status API.
                      </td>
                    </tr>
                """, errors.size() - 10));
            }

            errorRowsHtml.append("</tbody></table>");
        }

        String statusBadgeColor = failed == 0 ? "#16a34a" : (successful > 0 ? "#f59e0b" : "#dc2626");
        String statusLabel = failed == 0 ? "SUCCESS" : (successful > 0 ? "PARTIAL SUCCESS" : "FAILED");

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
              <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background-color: #f4f7f6; }
                .container { max-width: 620px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
                .header { background: #1e293b; padding: 30px; color: #ffffff; }
                .header h1 { margin: 0 0 6px 0; font-size: 22px; }
                .badge { display: inline-block; padding: 4px 12px; border-radius: 9999px; font-size: 12px; font-weight: 700; color: #ffffff; background: %s; }
                .content { padding: 28px 30px; }
                .metrics-grid { display: flex; gap: 12px; margin: 20px 0; }
                .metric-card { flex: 1; padding: 14px; border-radius: 8px; background: #f8fafc; border: 1px solid #e2e8f0; text-align: center; }
                .metric-value { font-size: 24px; font-weight: 700; margin-top: 4px; }
                .footer { background: #f8fafc; padding: 18px; text-align: center; font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <span class="badge">%s</span>
                  <h1>%s Bulk Upload Summary</h1>
                  <p style="margin: 4px 0 0 0; color: #94a3b8; font-size: 13px;">Job ID: %s</p>
                </div>
                <div class="content">
                  <p>The asynchronous bulk upload process has completed. Here is the processing summary:</p>
                  
                  <table style="width: 100%%; border: 1px solid #e2e8f0; border-radius: 8px; text-align: center; margin: 18px 0;">
                    <tr>
                      <td style="padding: 14px; border-right: 1px solid #e2e8f0;">
                        <div style="font-size: 12px; color: #64748b; text-transform: uppercase;">Total Rows</div>
                        <div style="font-size: 24px; font-weight: bold; color: #1e293b;">%d</div>
                      </td>
                      <td style="padding: 14px; border-right: 1px solid #e2e8f0;">
                        <div style="font-size: 12px; color: #16a34a; text-transform: uppercase;">Successful</div>
                        <div style="font-size: 24px; font-weight: bold; color: #16a34a;">%d</div>
                      </td>
                      <td style="padding: 14px;">
                        <div style="font-size: 12px; color: #dc2626; text-transform: uppercase;">Failed</div>
                        <div style="font-size: 24px; font-weight: bold; color: #dc2626;">%d</div>
                      </td>
                    </tr>
                  </table>

                  %s
                  
                  <p style="margin-top: 24px; font-size: 13px; color: #64748b;">
                    You can query the full job error details anytime via the API:<br>
                    <code>GET /api/v1/%s/bulk-upload/failed-rows/%s</code>
                  </p>
                </div>
                <div class="footer">
                  © 2026 Demo Application. Bulk Processing Engine.
                </div>
              </div>
            </body>
            </html>
            """.formatted(statusBadgeColor, statusLabel, jobType, jobId, total, successful, failed,
                          errorRowsHtml.toString(), jobType.toLowerCase(), jobId);

        sendGenericEmail(toEmail, subject, html);
    }

    @Async("notificationExecutor")
    @Override
    public void sendGenericEmail(String toEmail, String subject, String htmlBody) {
        if (!notificationFeatureEnabled) {
            log.info("[NOTIFICATION DISABLED] Master switch is disabled in application.properties/yml. Skipping email to: {}", toEmail);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        boolean isPlaceholder = isPlaceholderSmtp();

        if (isPlaceholder) {
            log.warn("""
                \n==================== [LIVE EMAIL: SMTP CONFIGURATION NEEDED] ====================
                To deliver real emails to user inboxes (attempted recipient: {}):
                Please set your valid Gmail and 16-character Google App Password in
                src/main/resources/application.properties:
                  spring.mail.username=your_email@gmail.com
                  spring.mail.password=your_16_digit_app_password
                Currently running in mock simulation mode.
                ================================================================================
                """, toEmail);
        }

        if (emailEnabled && mailSender != null && !isPlaceholder) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(toEmail);
                helper.setSubject(subject);
                helper.setText(htmlBody, true);

                mailSender.send(message);
                log.info("Email delivered successfully to: [{}] with subject: [{}]", toEmail, subject);
                return;
            } catch (Exception ex) {
                log.error("Failed to send email to [{}] via JavaMailSender: {}. Falling back to preview log.",
                        toEmail, ex.getMessage(), ex);
            }
        }

        // Preview logging when email is disabled or SMTP credentials need configuration
        String previewStatus = !emailEnabled
                ? "MOCK_MODE (app.notification.email.enabled=false)"
                : (isPlaceholder
                    ? "MOCK_MODE (SMTP credentials not configured in application.properties)"
                    : "ATTEMPTED_SMTP_FALLBACK");

        log.info("""
            \n==================== [ASYNC EMAIL DISPATCH PREVIEW] ====================
            To: {}
            From: {}
            Subject: {}
            Status: {}
            Content Preview:
            {}
            ========================================================================
            """,
            toEmail,
            fromEmail,
            subject,
            previewStatus,
            htmlBody.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim().substring(0, Math.min(250, htmlBody.length())) + "..."
        );
    }

    private boolean isPlaceholderSmtp() {
        if (smtpUsername == null || smtpUsername.isBlank() || smtpPassword == null || smtpPassword.isBlank()) {
            return true;
        }
        String u = smtpUsername.toLowerCase().trim();
        String p = smtpPassword.toLowerCase().trim();
        return u.contains("your-email") || u.contains("your_email") || u.contains("your-real") || u.contains("example.com")
                || p.contains("your-app-password") || p.contains("your_16_digit") || p.equals("secret") || p.equals("password");
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}
