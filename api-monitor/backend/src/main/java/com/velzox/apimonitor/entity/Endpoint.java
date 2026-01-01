package com.velzox.apimonitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Endpoint Entity - Represents an API endpoint being monitored
 * 
 * Each endpoint has:
 * - URL to monitor
 * - HTTP Method (GET/POST)
 * - Optional headers and request body
 * - Expected status code
 * - Check interval (based on plan limits)
 * 
 * The monitoring system will:
 * - Periodically send requests
 * - Measure latency and status
 * - Store results and trigger alerts on failures
 */
@Entity
@Table(name = "endpoints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Endpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name for the endpoint
     * Example: "User Login API", "Health Check", "Payment Process"
     */
    @Column(nullable = false)
    private String name;

    /**
     * Full URL to monitor (must be accessible from the monitoring server)
     * Example: "https://api.example.com/v1/health"
     */
    @Column(nullable = false)
    private String url;

    /**
     * HTTP method to use for the check
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private HttpMethod method = HttpMethod.GET;

    /**
     * Custom headers to include in the request (stored as JSON)
     * Example: {"Content-Type": "application/json", "X-API-Key": "..."}
     * Note: Auth headers should use Credential reference instead
     */
    @Column(columnDefinition = "TEXT")
    private String headers;

    /**
     * Request body for POST requests (JSON format)
     * Limited to max-body-size-bytes configuration
     */
    @Column(columnDefinition = "TEXT")
    private String requestBody;

    /**
     * Expected HTTP status code for a successful check
     * Default: 200 OK
     */
    @Builder.Default
    private int expectedStatusCode = 200;

    /**
     * Check interval in seconds (must respect plan limits)
     * FREE: min 300s (5 min)
     * STARTER: min 60s (1 min)
     * PRO: min 30s
     */
    @Builder.Default
    private int checkIntervalSeconds = 300;

    /**
     * Request timeout in milliseconds
     * If the request takes longer, it's considered a timeout failure
     */
    @Builder.Default
    private int timeoutMs = 30000;

    /**
     * Maximum acceptable latency in milliseconds
     * If exceeded, triggers a "Performance Degradation" alert
     */
    private Integer maxLatencyMs;

    /**
     * Reference to stored credential for authentication
     * Uses encrypted credential from the credentials manager
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id")
    private Credential credential;

    /**
     * Project this endpoint belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * Check results history for this endpoint
     */
    @OneToMany(mappedBy = "endpoint", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CheckResult> checkResults = new ArrayList<>();

    /**
     * Current status of the endpoint
     */
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EndpointStatus status = EndpointStatus.UNKNOWN;

    /**
     * Whether monitoring is enabled for this endpoint
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * Timestamp of the last check
     */
    private LocalDateTime lastCheckAt;

    /**
     * Timestamp of the next scheduled check
     */
    private LocalDateTime nextCheckAt;

    /**
     * Consecutive failure count (for alert triggering)
     */
    @Builder.Default
    private int consecutiveFailures = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Supported HTTP methods for monitoring
     */
    public enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH,
        HEAD
    }

    /**
     * Current status of the endpoint based on recent checks
     */
    public enum EndpointStatus {
        UP,           // Endpoint is responding correctly
        DOWN,         // Endpoint is failing
        DEGRADED,     // Endpoint is slow but working
        UNKNOWN       // No checks performed yet
    }

    /**
     * Reset failure counter when endpoint recovers
     */
    public void resetFailures() {
        this.consecutiveFailures = 0;
        this.status = EndpointStatus.UP;
    }

    /**
     * Increment failure counter
     */
    public void incrementFailures() {
        this.consecutiveFailures++;
        this.status = EndpointStatus.DOWN;
    }
}
