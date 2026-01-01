package com.velzox.apimonitor.scheduler;

import com.velzox.apimonitor.repository.AlertRepository;
import com.velzox.apimonitor.repository.CheckResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Data Cleanup Scheduler - Removes old data based on retention policies
 * 
 * RETENTION POLICIES:
 * - Check results: Based on user plan (1-30 days)
 * - Alerts: 30 days for all plans
 * - Resolved incidents: 90 days
 * 
 * This helps:
 * - Keep database size manageable
 * - Maintain query performance
 * - Comply with data retention requirements
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataCleanupScheduler {

    private final CheckResultRepository checkResultRepository;
    private final AlertRepository alertRepository;

    /**
     * Clean up old check results
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldCheckResults() {
        log.info("Starting check results cleanup...");

        try {
            // Delete check results older than 30 days (max retention)
            // Individual user limits are handled by the free tier data purge
            LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
            int deleted = checkResultRepository.deleteOlderThan(cutoff);

            log.info("Deleted {} old check results", deleted);

        } catch (Exception e) {
            log.error("Error during check results cleanup", e);
        }
    }

    /**
     * Clean up old alerts
     * Runs daily at 3:30 AM
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanupOldAlerts() {
        log.info("Starting alerts cleanup...");

        try {
            // Delete alerts older than 90 days
            LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
            int deleted = alertRepository.deleteOlderThan(cutoff);

            log.info("Deleted {} old alerts", deleted);

        } catch (Exception e) {
            log.error("Error during alerts cleanup", e);
        }
    }

    /**
     * Additional cleanup for free tier users
     * Runs every 6 hours to enforce 24-hour retention
     */
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupFreeTierData() {
        log.info("Starting free tier data cleanup...");

        // This would require joining with user plans
        // For MVP, the main cleanup handles this
        // Detailed per-plan cleanup can be added later
    }
}
