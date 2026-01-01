package com.velzox.apimonitor.repository;

import com.velzox.apimonitor.entity.CheckResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for CheckResult entity operations
 * 
 * Provides methods for:
 * - Storing check results
 * - Fetching history for endpoints
 * - Calculating uptime and latency statistics
 * - Cleanup of old results (based on plan limits)
 */
@Repository
public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    /**
     * Find recent check results for an endpoint
     */
    Page<CheckResult> findByEndpointIdOrderByCreatedAtDesc(Long endpointId, Pageable pageable);

    /**
     * Find check results for an endpoint within a time range
     */
    List<CheckResult> findByEndpointIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long endpointId, LocalDateTime start, LocalDateTime end);

    /**
     * Get the latest check result for an endpoint
     */
    @Query("SELECT cr FROM CheckResult cr WHERE cr.endpoint.id = :endpointId " +
           "ORDER BY cr.createdAt DESC LIMIT 1")
    CheckResult findLatestByEndpointId(Long endpointId);

    /**
     * Calculate uptime percentage for an endpoint in a time period
     * Uptime = (successful checks / total checks) * 100
     */
    @Query("SELECT " +
           "CAST(SUM(CASE WHEN cr.success = true THEN 1 ELSE 0 END) AS double) / " +
           "CAST(COUNT(cr) AS double) * 100 " +
           "FROM CheckResult cr " +
           "WHERE cr.endpoint.id = :endpointId " +
           "AND cr.createdAt >= :since")
    Double calculateUptimePercentage(Long endpointId, LocalDateTime since);

    /**
     * Calculate average latency for an endpoint in a time period
     */
    @Query("SELECT AVG(cr.latencyMs) FROM CheckResult cr " +
           "WHERE cr.endpoint.id = :endpointId " +
           "AND cr.createdAt >= :since " +
           "AND cr.success = true")
    Double calculateAverageLatency(Long endpointId, LocalDateTime since);

    /**
     * Get latency percentiles for an endpoint
     * Returns [min, p50, p95, p99, max]
     */
    @Query(value = "SELECT " +
           "MIN(latency_ms), " +
           "PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY latency_ms), " +
           "PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY latency_ms), " +
           "PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY latency_ms), " +
           "MAX(latency_ms) " +
           "FROM check_results " +
           "WHERE endpoint_id = :endpointId AND created_at >= :since AND success = true",
           nativeQuery = true)
    List<Object[]> calculateLatencyPercentiles(Long endpointId, LocalDateTime since);

    /**
     * Count total checks for an endpoint in a time period
     */
    long countByEndpointIdAndCreatedAtAfter(Long endpointId, LocalDateTime since);

    /**
     * Count successful checks for an endpoint in a time period
     */
    long countByEndpointIdAndSuccessTrueAndCreatedAtAfter(Long endpointId, LocalDateTime since);

    /**
     * Count failed checks for an endpoint in a time period
     */
    long countByEndpointIdAndSuccessFalseAndCreatedAtAfter(Long endpointId, LocalDateTime since);

    /**
     * Get failure breakdown by type for an endpoint
     */
    @Query("SELECT cr.resultType, COUNT(cr) FROM CheckResult cr " +
           "WHERE cr.endpoint.id = :endpointId " +
           "AND cr.success = false " +
           "AND cr.createdAt >= :since " +
           "GROUP BY cr.resultType")
    List<Object[]> getFailureBreakdown(Long endpointId, LocalDateTime since);

    /**
     * Delete old check results (for data retention cleanup)
     * Called by scheduled job to enforce plan-based history limits
     */
    @Modifying
    @Query("DELETE FROM CheckResult cr WHERE cr.createdAt < :before")
    int deleteOlderThan(LocalDateTime before);

    /**
     * Delete old check results for a specific endpoint
     */
    @Modifying
    @Query("DELETE FROM CheckResult cr " +
           "WHERE cr.endpoint.id = :endpointId AND cr.createdAt < :before")
    int deleteByEndpointIdOlderThan(Long endpointId, LocalDateTime before);

    /**
     * Get check results grouped by hour for charts
     */
    @Query("SELECT FUNCTION('DATE_TRUNC', 'hour', cr.createdAt) as hour, " +
           "COUNT(cr), " +
           "SUM(CASE WHEN cr.success = true THEN 1 ELSE 0 END), " +
           "AVG(cr.latencyMs) " +
           "FROM CheckResult cr " +
           "WHERE cr.endpoint.id = :endpointId " +
           "AND cr.createdAt >= :since " +
           "GROUP BY FUNCTION('DATE_TRUNC', 'hour', cr.createdAt) " +
           "ORDER BY hour")
    List<Object[]> getHourlyStats(Long endpointId, LocalDateTime since);

    /**
     * Find the last failure time for an endpoint
     */
    @Query("SELECT MAX(cr.createdAt) FROM CheckResult cr " +
           "WHERE cr.endpoint.id = :endpointId AND cr.success = false")
    LocalDateTime findLastFailureTime(Long endpointId);
}
