package com.velzox.apimonitor.repository;

import com.velzox.apimonitor.entity.Incident;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Incident entity operations
 * 
 * Provides methods for:
 * - Incident management
 * - Open incident tracking
 * - Incident resolution
 */
@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /**
     * Find open incident for an endpoint
     * There should only be one open incident per endpoint at a time
     */
    Optional<Incident> findByEndpointIdAndStatus(Long endpointId, Incident.IncidentStatus status);

    /**
     * Find all open incidents for a user's endpoints
     */
    @Query("SELECT i FROM Incident i " +
           "WHERE i.endpoint.project.owner.id = :userId " +
           "AND i.status = 'OPEN' " +
           "ORDER BY i.startedAt DESC")
    List<Incident> findOpenIncidentsByUserId(Long userId);

    /**
     * Find incidents for an endpoint, ordered by start time
     */
    Page<Incident> findByEndpointIdOrderByStartedAtDesc(Long endpointId, Pageable pageable);

    /**
     * Find incidents for a user across all endpoints
     */
    @Query("SELECT i FROM Incident i " +
           "WHERE i.endpoint.project.owner.id = :userId " +
           "ORDER BY i.startedAt DESC")
    Page<Incident> findByUserId(Long userId, Pageable pageable);

    /**
     * Count open incidents for a user
     */
    @Query("SELECT COUNT(i) FROM Incident i " +
           "WHERE i.endpoint.project.owner.id = :userId " +
           "AND i.status = 'OPEN'")
    long countOpenIncidentsByUserId(Long userId);

    /**
     * Calculate total downtime minutes for an endpoint in a period
     */
    @Query("SELECT COALESCE(SUM(" +
           "FUNCTION('TIMESTAMPDIFF', MINUTE, i.startedAt, " +
           "COALESCE(i.resolvedAt, CURRENT_TIMESTAMP))), 0) " +
           "FROM Incident i " +
           "WHERE i.endpoint.id = :endpointId " +
           "AND i.startedAt >= :since")
    long calculateDowntimeMinutes(Long endpointId, LocalDateTime since);

    /**
     * Resolve open incident for an endpoint
     */
    @Modifying
    @Query("UPDATE Incident i SET i.status = 'RESOLVED', i.resolvedAt = :now " +
           "WHERE i.endpoint.id = :endpointId AND i.status = 'OPEN'")
    int resolveOpenIncident(Long endpointId, LocalDateTime now);

    /**
     * Update incident failure count
     */
    @Modifying
    @Query("UPDATE Incident i SET i.failedCheckCount = i.failedCheckCount + 1, " +
           "i.lastErrorMessage = :errorMessage " +
           "WHERE i.id = :incidentId")
    void incrementFailureCount(Long incidentId, String errorMessage);

    /**
     * Find incidents by status
     */
    List<Incident> findByStatusOrderByStartedAtDesc(Incident.IncidentStatus status);

    /**
     * Get incident statistics for a user in a time period
     */
    @Query("SELECT COUNT(i), " +
           "SUM(CASE WHEN i.status = 'RESOLVED' THEN 1 ELSE 0 END), " +
           "AVG(FUNCTION('TIMESTAMPDIFF', MINUTE, i.startedAt, " +
           "COALESCE(i.resolvedAt, CURRENT_TIMESTAMP))) " +
           "FROM Incident i " +
           "WHERE i.endpoint.project.owner.id = :userId " +
           "AND i.startedAt >= :since")
    Object[] getIncidentStats(Long userId, LocalDateTime since);
}
