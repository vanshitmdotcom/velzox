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
 * Incident Entity - Groups related failures into a single incident
 * 
 * INCIDENT-BASED ALERTING:
 * Instead of spamming alerts for every failed check, we:
 * 1. Create an incident when first failure occurs
 * 2. Group subsequent failures into the same incident
 * 3. Send one alert per incident (with updates if needed)
 * 4. Close incident when endpoint recovers
 * 
 * This prevents alert fatigue and provides clearer incident tracking.
 */
@Entity
@Table(name = "incidents", indexes = {
    @Index(name = "idx_incidents_endpoint_status", columnList = "endpoint_id, status"),
    @Index(name = "idx_incidents_started_at", columnList = "started_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The endpoint experiencing the incident
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id", nullable = false)
    private Endpoint endpoint;

    /**
     * Current status of the incident
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    /**
     * Type of failure that caused the incident
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckResult.CheckResultType failureType;

    /**
     * When the incident started (first failure)
     */
    @Column(nullable = false)
    private LocalDateTime startedAt;

    /**
     * When the incident was resolved (endpoint recovered)
     */
    private LocalDateTime resolvedAt;

    /**
     * Total number of failed checks during this incident
     */
    @Builder.Default
    private int failedCheckCount = 0;

    /**
     * Alerts sent for this incident
     */
    @OneToMany(mappedBy = "incidentId", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Alert> alerts = new ArrayList<>();

    /**
     * Last error message from the most recent failure
     */
    @Column(columnDefinition = "TEXT")
    private String lastErrorMessage;

    /**
     * Notes added by the user (for documentation)
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Incident lifecycle states
     */
    public enum IncidentStatus {
        /**
         * Incident is ongoing - endpoint is still failing
         */
        OPEN,

        /**
         * User acknowledged the incident
         */
        ACKNOWLEDGED,

        /**
         * Incident resolved - endpoint recovered
         */
        RESOLVED
    }

    /**
     * Calculate incident duration in minutes
     */
    public long getDurationMinutes() {
        LocalDateTime endTime = resolvedAt != null ? resolvedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).toMinutes();
    }

    /**
     * Increment the failed check counter
     */
    public void recordFailure(String errorMessage) {
        this.failedCheckCount++;
        this.lastErrorMessage = errorMessage;
    }

    /**
     * Resolve the incident
     */
    public void resolve() {
        this.status = IncidentStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
    }

    /**
     * Create a new incident for an endpoint failure
     */
    public static Incident create(Endpoint endpoint, CheckResult.CheckResultType failureType, 
                                   String errorMessage) {
        return Incident.builder()
                .endpoint(endpoint)
                .status(IncidentStatus.OPEN)
                .failureType(failureType)
                .startedAt(LocalDateTime.now())
                .failedCheckCount(1)
                .lastErrorMessage(errorMessage)
                .build();
    }
}
