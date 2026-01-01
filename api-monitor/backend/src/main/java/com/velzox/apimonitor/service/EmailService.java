package com.velzox.apimonitor.service;

import com.velzox.apimonitor.entity.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

/**
 * Email Service - Sends alert notifications via email
 * 
 * FEATURES:
 * - HTML email templates for better readability
 * - Plain text fallback
 * - Different templates for different alert types
 * - Rate limiting (handled by alert service)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@apimonitor.velzox.com}")
    private String fromAddress;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Send an alert email to the user
     * 
     * @param alert The alert to send
     */
    public void sendAlertEmail(Alert alert) {
        // Skip if mail not configured
        if (mailUsername == null || mailUsername.isEmpty()) {
            log.warn("Email not configured - skipping email for alert {}", alert.getId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(alert.getUser().getEmail());
            helper.setSubject(alert.getTitle());

            // Build HTML content
            String htmlContent = buildHtmlEmail(alert);
            String textContent = alert.getMessage();

            helper.setText(textContent, htmlContent);

            mailSender.send(message);
            log.info("Alert email sent to {} for alert {}", 
                    alert.getUser().getEmail(), alert.getId());

        } catch (Exception e) {
            log.error("Failed to send alert email: {}", e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Build HTML email content for an alert
     */
    private String buildHtmlEmail(Alert alert) {
        String backgroundColor = getBackgroundColor(alert.getSeverity());
        String statusColor = getStatusColor(alert.getSeverity());
        String statusText = alert.getType().name().replace("_", " ");

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f5f5f5;">
                <table cellpadding="0" cellspacing="0" width="100%%" style="max-width: 600px; margin: 20px auto; background-color: #ffffff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <!-- Header -->
                    <tr>
                        <td style="background-color: %s; padding: 24px; border-radius: 8px 8px 0 0;">
                            <h1 style="margin: 0; color: #ffffff; font-size: 20px; font-weight: 600;">
                                %s
                            </h1>
                        </td>
                    </tr>
                    
                    <!-- Status Badge -->
                    <tr>
                        <td style="padding: 24px 24px 0 24px;">
                            <span style="display: inline-block; padding: 6px 12px; background-color: %s; color: #ffffff; border-radius: 4px; font-size: 12px; font-weight: 600; text-transform: uppercase;">
                                %s
                            </span>
                        </td>
                    </tr>
                    
                    <!-- Content -->
                    <tr>
                        <td style="padding: 24px;">
                            <table cellpadding="0" cellspacing="0" width="100%%" style="border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 12px 0; border-bottom: 1px solid #eee;">
                                        <span style="color: #666; font-size: 14px;">Endpoint</span><br>
                                        <strong style="color: #333; font-size: 16px;">%s</strong>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 12px 0; border-bottom: 1px solid #eee;">
                                        <span style="color: #666; font-size: 14px;">URL</span><br>
                                        <code style="color: #0066cc; font-size: 14px; word-break: break-all;">%s</code>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding: 12px 0; border-bottom: 1px solid #eee;">
                                        <span style="color: #666; font-size: 14px;">Time</span><br>
                                        <strong style="color: #333; font-size: 16px;">%s</strong>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Message -->
                            <div style="margin-top: 24px; padding: 16px; background-color: #f8f9fa; border-radius: 4px; border-left: 4px solid %s;">
                                <pre style="margin: 0; font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace; font-size: 13px; color: #333; white-space: pre-wrap; word-break: break-word;">%s</pre>
                            </div>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="padding: 24px; background-color: #f8f9fa; border-radius: 0 0 8px 8px; text-align: center;">
                            <p style="margin: 0 0 12px 0; color: #666; font-size: 14px;">
                                View your dashboard for more details
                            </p>
                            <p style="margin: 0; color: #999; font-size: 12px;">
                                API Monitor by Velzox Tech
                            </p>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """,
            backgroundColor,
            alert.getTitle(),
            statusColor,
            statusText,
            alert.getEndpoint().getName(),
            alert.getEndpoint().getUrl(),
            alert.getCreatedAt().format(DATE_FORMATTER),
            statusColor,
            escapeHtml(alert.getMessage())
        );
    }

    /**
     * Get background color based on severity
     */
    private String getBackgroundColor(Alert.AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "#dc2626";  // Red
            case ERROR -> "#ea580c";      // Orange
            case WARNING -> "#ca8a04";    // Yellow
            case INFO -> "#16a34a";       // Green
        };
    }

    /**
     * Get status color based on severity
     */
    private String getStatusColor(Alert.AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "#dc2626";
            case ERROR -> "#ea580c";
            case WARNING -> "#ca8a04";
            case INFO -> "#16a34a";
        };
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
