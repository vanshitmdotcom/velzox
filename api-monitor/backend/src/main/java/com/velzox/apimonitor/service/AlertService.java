package com.velzox.apimonitor.service;

import com.velzox.apimonitor.entity.*;
import com.velzox.apimonitor.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Alert Service - Manages alert creation and delivery
 * 
 * ALERT DESIGN:
 * - Incident-based alerts (not one per check failure)
 * - Deduplication to prevent spam
 * - Multiple channels: Email (MVP), Slack/Webhook (PRO)
 * - Recovery alerts when endpoints come back up
 * 
 * ALERT TYPES:
 * - ENDPOINT_DOWN: Endpoint is failing
 * - ENDPOINT_RECOVERED: Endpoint recovered from failure
 * - AUTH_FAILURE: 401 Unauthorized
 * - TIMEOUT: Request timed out
 * - SSL_ERROR: Certificate issues
 * - LATENCY_BREACH: Response time exceeded threshold
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final EmailService emailService;

    @Value("${app.alerting.failure-threshold:3}")
    private int failureThreshold;

    @Value("${app.alerting.deduplication-window-minutes:15}")
    private int deduplicationWindowMinutes;

    @Value("${app.alerting.email-enabled:true}")
    private boolean emailEnabled;

    /**
     * Send a failure alert for an endpoint
     * 
     * Only sends if:
     * - Failure threshold is reached
     * - No similar alert sent recently (deduplication)
     * 
     * @param endpoint The failing endpoint
     * @param result The check result
     * @param incident The related incident
     */
    @Async("alertExecutor")
    @Transactional
    public void sendFailureAlert(Endpoint endpoint, CheckResult result, Incident incident) {
        log.debug("Evaluating failure alert for endpoint {}", endpoint.getId());

        // Check if failure threshold reached
        if (endpoint.getConsecutiveFailures() < failureThreshold) {
            log.debug("Failure threshold not reached ({}/{})", 
                     endpoint.getConsecutiveFailures(), failureThreshold);
            return;
        }

        // Determine alert type
        Alert.AlertType alertType = Alert.fromCheckResultType(result.getResultType());

        // Check for deduplication
        LocalDateTime since = LocalDateTime.now().minusMinutes(deduplicationWindowMinutes);
        if (alertRepository.existsRecentAlert(endpoint.getId(), alertType, since)) {
            log.debug("Skipping duplicate alert for endpoint {}", endpoint.getId());
            return;
        }

        // Create and send alert
        Alert alert = createAlert(endpoint, result, alertType, incident);
        sendAlert(alert);
    }

    /**
     * Send a recovery alert when an endpoint recovers
     * 
     * @param endpoint The recovered endpoint
     */
    @Async("alertExecutor")
    @Transactional
    public void sendRecoveryAlert(Endpoint endpoint) {
        log.info("Sending recovery alert for endpoint {}", endpoint.getId());

        User user = endpoint.getProject().getOwner();

        Alert alert = Alert.builder()
                .endpoint(endpoint)
                .user(user)
                .type(Alert.AlertType.ENDPOINT_RECOVERED)
                .severity(Alert.AlertSeverity.INFO)
                .title("✅ Recovered: " + endpoint.getName())
                .message(generateRecoveryMessage(endpoint))
                .channel(Alert.AlertChannel.EMAIL)
                .build();

        alert = alertRepository.save(alert);
        sendAlert(alert);
    }

    /**
     * Acknowledge an alert
     * 
     * @param alertId Alert ID
     * @param userId User ID (for access control)
     */
    @Transactional
    public void acknowledgeAlert(Long alertId, Long userId) {
        log.info("Acknowledging alert {}", alertId);
        alertRepository.acknowledgeAlert(alertId, LocalDateTime.now());
    }

    /**
     * Acknowledge all alerts for an endpoint
     * 
     * @param endpointId Endpoint ID
     * @param userId User ID (for access control)
     */
    @Transactional
    public void acknowledgeAllForEndpoint(Long endpointId, Long userId) {
        log.info("Acknowledging all alerts for endpoint {}", endpointId);
        alertRepository.acknowledgeAllForEndpoint(endpointId, LocalDateTime.now());
    }

    /**
     * Create an alert entity
     */
    private Alert createAlert(Endpoint endpoint, CheckResult result, 
                              Alert.AlertType alertType, Incident incident) {
        User user = endpoint.getProject().getOwner();
        Alert.AlertSeverity severity = Alert.determineSeverity(alertType);

        String title = generateAlertTitle(alertType, endpoint);
        String message = Alert.generateMessage(endpoint, result);

        Alert alert = Alert.builder()
                .endpoint(endpoint)
                .user(user)
                .type(alertType)
                .severity(severity)
                .title(title)
                .message(message)
                .channel(Alert.AlertChannel.EMAIL)
                .incidentId(incident.getId())
                .build();

        return alertRepository.save(alert);
    }

    /**
     * Send an alert through the configured channel
     */
    private void sendAlert(Alert alert) {
        if (!emailEnabled) {
            log.warn("Email alerts disabled - skipping alert {}", alert.getId());
            return;
        }

        try {
            // Send email alert
            emailService.sendAlertEmail(alert);

            // Mark as delivered
            alert.setDelivered(true);
            alertRepository.save(alert);

            log.info("Alert sent successfully: {} ({})", alert.getId(), alert.getType());

        } catch (Exception e) {
            log.error("Failed to send alert {}: {}", alert.getId(), e.getMessage());
            
            // Record delivery failure
            alert.setDelivered(false);
            alert.setDeliveryError(e.getMessage());
            alertRepository.save(alert);
        }
    }

    /**
     * Generate alert title based on type
     */
    private String generateAlertTitle(Alert.AlertType type, Endpoint endpoint) {
        String emoji = getAlertEmoji(type);
        String action = getAlertAction(type);
        return String.format("%s %s: %s", emoji, action, endpoint.getName());
    }

    /**
     * Get emoji for alert type
     */
    private String getAlertEmoji(Alert.AlertType type) {
        return switch (type) {
            case ENDPOINT_DOWN -> "🔴";
            case ENDPOINT_RECOVERED -> "✅";
            case AUTH_FAILURE -> "🔐";
            case TIMEOUT -> "⏱️";
            case SSL_ERROR -> "🔒";
            case LATENCY_BREACH -> "🐢";
            case CONNECTION_ERROR -> "🔌";
        };
    }

    /**
     * Get action text for alert type
     */
    private String getAlertAction(Alert.AlertType type) {
        return switch (type) {
            case ENDPOINT_DOWN -> "API Down";
            case ENDPOINT_RECOVERED -> "Recovered";
            case AUTH_FAILURE -> "Auth Failed";
            case TIMEOUT -> "Timeout";
            case SSL_ERROR -> "SSL Error";
            case LATENCY_BREACH -> "Slow Response";
            case CONNECTION_ERROR -> "Connection Failed";
        };
    }

    /**
     * Generate recovery message
     */
    private String generateRecoveryMessage(Endpoint endpoint) {
        return String.format("""
            Good news! Your API endpoint is back online.
            
            Endpoint: %s
            URL: %s
            Status: UP
            Time: %s
            
            The endpoint is now responding correctly.
            """,
            endpoint.getName(),
            endpoint.getUrl(),
            LocalDateTime.now()
        );
    }
}
