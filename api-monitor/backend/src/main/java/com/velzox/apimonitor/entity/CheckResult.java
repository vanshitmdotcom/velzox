package com.velzox.apimonitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * CheckResult Entity - Stores the result of each API endpoint check
 * 
 * Each check records:
 * - HTTP status code returned
 * - Response latency in milliseconds
 * - Success/failure status
 * - Failure reason (if applicable)
 * 
 * Results are retained based on user's plan:
 * - FREE: 24 hours
 * - STARTER: 7 days
 * - PRO: 30 days
 * 
 * NOTE: Response bodies are NOT stored for:
 * - Security (may contain sensitive data)
 * - Storage efficiency
 * - Privacy compliance
 */
@Entity
@Table(name = "check_results", indexes = {
    @Index(name = "idx_check_results_endpoint_created", columnList = "endpoint_id, created_at DESC"),
    @Index(name = "idx_check_results_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The endpoint that was checked
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    /**
     * HTTP status code returned by the endpoint
     * 0 indicates connection failure/timeout
     */
    private int statusCode;

    /**
     * Response time in milliseconds
     * Measures time from request sent to response received
     */
    private long latencyMs;

    /**
     * Whether the check was successful
     * Success = correct status code AND within latency threshold
     */
    private boolean success;

    /**
     * Type of check result/failure
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckResultType resultType;

    /**
     * Human-readable error message (if failed)
     * Examples:
     * - "Connection timeout after 30000ms"
     * - "Expected status 200 but got 500"
     * - "SSL certificate expired"
     */
    @Column(length = 1000)
    private String errorMessage;

    /**
     * Timestamp when the check was performed
     */
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * Types of check results for classification
     */
    public enum CheckResultType {
        /**
         * Check succeeded - correct status and acceptable latency
         */
        SUCCESS,

        /**
         * Wrong HTTP status code returned
         */
        STATUS_MISMATCH,

        /**
         * Request timed out
         */
        TIMEOUT,

        /**
         * Connection failed (DNS, network, etc.)
         */
        CONNECTION_ERROR,

        /**
         * SSL/TLS error (certificate issues)
         */
        SSL_ERROR,

        /**
         * Authentication failed (401 Unauthorized)
         */
        AUTH_FAILURE,

        /**
         * Latency exceeded threshold (endpoint is slow)
         */
        LATENCY_BREACH,

        /**
         * Server error (5xx status codes)
         */
        SERVER_ERROR,

        /**
         * Unknown/unexpected error
         */
        UNKNOWN_ERROR
    }

    /**
     * Factory method to create a successful check result
     */
    public static CheckResult success(Endpoint endpoint, int statusCode, long latencyMs) {
        return CheckResult.builder()
                .endpoint(endpoint)
                .statusCode(statusCode)
                .latencyMs(latencyMs)
                .success(true)
                .resultType(CheckResultType.SUCCESS)
                .build();
    }

    /**
     * Factory method to create a failed check result
     */
    public static CheckResult failure(Endpoint endpoint, CheckResultType type, 
                                       int statusCode, long latencyMs, String errorMessage) {
        return CheckResult.builder()
                .endpoint(endpoint)
                .statusCode(statusCode)
                .latencyMs(latencyMs)
                .success(false)
                .resultType(type)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * Determine the result type based on the check outcome
     */
    public static CheckResultType classifyResult(int statusCode, int expectedStatus, 
                                                  long latencyMs, Integer maxLatencyMs,
                                                  Exception error) {
        if (error != null) {
            String errorMsg = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
            if (errorMsg.contains("timeout")) {
                return CheckResultType.TIMEOUT;
            }
            if (errorMsg.contains("ssl") || errorMsg.contains("certificate")) {
                return CheckResultType.SSL_ERROR;
            }
            if (errorMsg.contains("connection") || errorMsg.contains("refused")) {
                return CheckResultType.CONNECTION_ERROR;
            }
            return CheckResultType.UNKNOWN_ERROR;
        }

        if (statusCode == 401) {
            return CheckResultType.AUTH_FAILURE;
        }

        if (statusCode >= 500) {
            return CheckResultType.SERVER_ERROR;
        }

        if (statusCode != expectedStatus) {
            return CheckResultType.STATUS_MISMATCH;
        }

        if (maxLatencyMs != null && latencyMs > maxLatencyMs) {
            return CheckResultType.LATENCY_BREACH;
        }

        return CheckResultType.SUCCESS;
    }
}
