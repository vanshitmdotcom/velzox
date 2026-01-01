package com.velzox.apimonitor.repository;

import com.velzox.apimonitor.entity.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Endpoint entity operations
 * 
 * Provides methods for:
 * - Fetching endpoints for monitoring scheduler
 * - Status updates
 * - Statistics and uptime calculations
 */
@Repository
public interface EndpointRepository extends JpaRepository<Endpoint, Long> {

    /**
     * Find all endpoints belonging to a project
     */
    List<Endpoint> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    /**
     * Find endpoint by ID and verify project ownership
     */
    @Query("SELECT e FROM Endpoint e WHERE e.id = :endpointId AND e.project.owner.id = :ownerId")
    Optional<Endpoint> findByIdAndOwnerId(Long endpointId, Long ownerId);

    /**
     * Find all enabled endpoints that are due for a check
     * Used by the monitoring scheduler to fetch endpoints needing checks
     */
    @Query("SELECT e FROM Endpoint e WHERE e.enabled = true " +
           "AND (e.nextCheckAt IS NULL OR e.nextCheckAt <= :now)")
    List<Endpoint> findEndpointsDueForCheck(LocalDateTime now);

    /**
     * Find all enabled endpoints (for scheduler initialization)
     */
    List<Endpoint> findByEnabledTrue();

    /**
     * Count endpoints by status for a project
     */
    @Query("SELECT e.status, COUNT(e) FROM Endpoint e " +
           "WHERE e.project.id = :projectId GROUP BY e.status")
    List<Object[]> countByStatusForProject(Long projectId);

    /**
     * Count endpoints by status for a user (across all projects)
     */
    @Query("SELECT e.status, COUNT(e) FROM Endpoint e " +
           "WHERE e.project.owner.id = :userId GROUP BY e.status")
    List<Object[]> countByStatusForUser(Long userId);

    /**
     * Update endpoint status and check timestamps
     */
    @Modifying
    @Query("UPDATE Endpoint e SET e.status = :status, e.lastCheckAt = :lastCheck, " +
           "e.nextCheckAt = :nextCheck, e.consecutiveFailures = :failures WHERE e.id = :id")
    void updateCheckStatus(Long id, Endpoint.EndpointStatus status, 
                          LocalDateTime lastCheck, LocalDateTime nextCheck, int failures);

    /**
     * Find endpoints with open incidents
     */
    @Query("SELECT DISTINCT e FROM Endpoint e " +
           "JOIN Incident i ON i.endpoint.id = e.id " +
           "WHERE i.status = 'OPEN' AND e.project.owner.id = :userId")
    List<Endpoint> findEndpointsWithOpenIncidents(Long userId);

    /**
     * Count total enabled endpoints for a user
     */
    @Query("SELECT COUNT(e) FROM Endpoint e " +
           "WHERE e.project.owner.id = :userId AND e.enabled = true")
    long countEnabledByUserId(Long userId);

    /**
     * Find endpoint with credential eagerly loaded
     */
    @Query("SELECT e FROM Endpoint e LEFT JOIN FETCH e.credential WHERE e.id = :id")
    Optional<Endpoint> findByIdWithCredential(Long id);
}
