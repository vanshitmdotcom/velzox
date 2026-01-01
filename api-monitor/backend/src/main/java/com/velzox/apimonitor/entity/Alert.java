package com.velzox.apimonitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Alert Entity - Records alert notifications sent to users
 * 
 * ALERT DESIGN:
 * - Alerts are incident-based, not spam
 * - Deduplication prevents repeated alerts for the same issue
 * - Different failure types generate different alert messages
 * - Backoff logic prevents alert fatigue
 * 
 * ALERT CHANNELS:
 * - Email (all plans)
 * - Slack webhook (PRO only)
 * - Custom webhook (PRO only)
 */
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alerts_endpoint_created", columnList = "endpoint_id, created_at DESC"),
    @Index(name = "idx_alerts_user_created", columnList = "user_id, created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The endpoint that triggered this alert
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    /**
     * The user who should receive this alert
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Type of alert
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;

    /**
     * Severity level
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    /**
     * Alert title for display
     * Example: "API Down: User Service Health Check"
     */
    @Column(nullable = false)
    private String title;

    /**
     * Detailed alert message
     * Includes failure reason, status code, latency, etc.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    /**
     * Channel through which this alert was sent
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertChannel channel;

    /**
     * Whether the alert was successfully delivered
     */
    @Builder.Default
    private boolean delivered = false;

    /**
     * Delivery error message (if failed)
     */
    @Column(length = 500)
    private String deliveryError;

    /**
     * Whether the user has acknowledged this alert
     */
    @Builder.Default
    private boolean acknowledged = false;

    /**
     * Timestamp when the alert was acknowledged
     */
    private LocalDateTime acknowledgedAt;

    /**
     * Related incident ID (for grouping related alerts)
     */
    private Long incidentId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Types of alerts based on failure classification
     */
    public enum AlertType {
        /**
         * Endpoint is returning errors (5xx, wrong status)
         */
        ENDPOINT_DOWN,

        /**
         * Endpoint recovered from failure
         */
        ENDPOINT_RECOVERED,

        /**
         * Authentication failure (401)
         */
        AUTH_FAILURE,

        /**
         * Request timeout
         */
        TIMEOUT,

        /**
         * SSL/TLS certificate issues
         */
        SSL_ERROR,

        /**
         * Latency exceeded threshold
         */
        LATENCY_BREACH,

        /**
         * Connection errors
         */
        CONNECTION_ERROR
    }

    /**
     * Alert severity levels
     */
    public enum AlertSeverity {
        /**
         * Informational (e.g., endpoint recovered)
         */
        INFO,

        /**
         * Warning (e.g., latency breach)
         */
        WARNING,

        /**
         * Error (e.g., endpoint down)
         */
        ERROR,

        /**
         * Critical (e.g., auth failure, SSL error)
         */
        CRITICAL
    }

    /**
     * Alert delivery channels
     */
    public enum AlertChannel {
        EMAIL,
        SLACK,
        WEBHOOK
    }

    /**
     * Map check result type to alert type
     */
    public static AlertType fromCheckResultType(CheckResult.CheckResultType resultType) {
        return switch (resultType) {
            case TIMEOUT -> AlertType.TIMEOUT;
            case AUTH_FAILURE -> AlertType.AUTH_FAILURE;
            case SSL_ERROR -> AlertType.SSL_ERROR;
            case LATENCY_BREACH -> AlertType.LATENCY_BREACH;
            case CONNECTION_ERROR -> AlertType.CONNECTION_ERROR;
            default -> AlertType.ENDPOINT_DOWN;
        };
    }

    /**
     * Determine severity based on alert type
     */
    public static AlertSeverity determineSeverity(AlertType type) {
        return switch (type) {
            case ENDPOINT_RECOVERED -> AlertSeverity.INFO;
            case LATENCY_BREACH -> AlertSeverity.WARNING;
            case AUTH_FAILURE, SSL_ERROR -> AlertSeverity.CRITICAL;
            default -> AlertSeverity.ERROR;
        };
    }

    /**
     * Generate a descriptive alert message based on the failure
     */
    public static String generateMessage(Endpoint endpoint, CheckResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Endpoint: ").append(endpoint.getName()).append("\n");
        sb.append("URL: ").append(endpoint.getUrl()).append("\n");
        sb.append("Status Code: ").append(result.getStatusCode()).append("\n");
        sb.append("Latency: ").append(result.getLatencyMs()).append("ms\n");
        
        if (result.getErrorMessage() != null) {
            sb.append("Error: ").append(result.getErrorMessage()).append("\n");
        }
        
        sb.append("Time: ").append(result.getCreatedAt()).append("\n");
        
        return sb.toString();
    }
}
