package com.velzox.apimonitor.repository;

import com.velzox.apimonitor.entity.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Alert entity operations
 * 
 * Provides methods for:
 * - Alert history and filtering
 * - Deduplication checks
 * - Alert acknowledgment
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * Find alerts for a user, ordered by creation time
     */
    Page<Alert> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find unacknowledged alerts for a user
     */
    List<Alert> findByUserIdAndAcknowledgedFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Find alerts for an endpoint, ordered by creation time
     */
    Page<Alert> findByEndpointIdOrderByCreatedAtDesc(Long endpointId, Pageable pageable);

    /**
     * Check if a similar alert was sent recently (for deduplication)
     * Returns true if an alert of the same type was sent within the window
     */
    @Query("SELECT COUNT(a) > 0 FROM Alert a " +
           "WHERE a.endpoint.id = :endpointId " +
           "AND a.type = :type " +
           "AND a.createdAt >= :since")
    boolean existsRecentAlert(Long endpointId, Alert.AlertType type, LocalDateTime since);

    /**
     * Find the last alert sent for an endpoint
     */
    @Query("SELECT a FROM Alert a " +
           "WHERE a.endpoint.id = :endpointId " +
           "ORDER BY a.createdAt DESC LIMIT 1")
    Alert findLatestByEndpointId(Long endpointId);

    /**
     * Count unacknowledged alerts for a user
     */
    long countByUserIdAndAcknowledgedFalse(Long userId);

    /**
     * Count alerts by severity for a user
     */
    @Query("SELECT a.severity, COUNT(a) FROM Alert a " +
           "WHERE a.user.id = :userId " +
           "AND a.createdAt >= :since " +
           "GROUP BY a.severity")
    List<Object[]> countBySeverity(Long userId, LocalDateTime since);

    /**
     * Acknowledge all alerts for an endpoint
     */
    @Modifying
    @Query("UPDATE Alert a SET a.acknowledged = true, a.acknowledgedAt = :now " +
           "WHERE a.endpoint.id = :endpointId AND a.acknowledged = false")
    int acknowledgeAllForEndpoint(Long endpointId, LocalDateTime now);

    /**
     * Acknowledge a specific alert
     */
    @Modifying
    @Query("UPDATE Alert a SET a.acknowledged = true, a.acknowledgedAt = :now " +
           "WHERE a.id = :alertId")
    int acknowledgeAlert(Long alertId, LocalDateTime now);

    /**
     * Find failed alert deliveries (for retry logic)
     */
    List<Alert> findByDeliveredFalseAndCreatedAtAfter(LocalDateTime since);

    /**
     * Delete old alerts (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM Alert a WHERE a.createdAt < :before")
    int deleteOlderThan(LocalDateTime before);

    /**
     * Find alerts for an incident
     */
    List<Alert> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
