package com.velzox.apimonitor.scheduler;

import com.velzox.apimonitor.entity.Endpoint;
import com.velzox.apimonitor.service.EndpointService;
import com.velzox.apimonitor.service.MonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Monitoring Scheduler - Periodically checks endpoints that are due
 * 
 * DESIGN:
 * - Runs every 10 seconds to check for due endpoints
 * - Executes checks asynchronously using thread pool
 * - Respects max concurrent checks to prevent overload
 * - Handles errors gracefully without stopping the scheduler
 * 
 * SCALING:
 * - Thread pool size configurable via properties
 * - Can handle thousands of endpoints with proper tuning
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonitoringScheduler {

    private final EndpointService endpointService;
    private final MonitoringService monitoringService;
    private final Executor monitoringExecutor;

    @Value("${app.monitoring.max-concurrent-checks}")
    private int maxConcurrentChecks;

    private final AtomicInteger activeChecks = new AtomicInteger(0);

    /**
     * Main scheduler method - runs every 10 seconds
     * 
     * Finds endpoints due for checking and executes checks asynchronously.
     */
    @Scheduled(fixedRate = 10000)  // Run every 10 seconds
    public void runScheduledChecks() {
        log.debug("Running scheduled check cycle...");

        try {
            // Get endpoints due for check
            List<Endpoint> dueEndpoints = endpointService.getEndpointsDueForCheck();

            if (dueEndpoints.isEmpty()) {
                log.debug("No endpoints due for check");
                return;
            }

            log.info("Found {} endpoints due for check", dueEndpoints.size());

            // Execute checks respecting max concurrent limit
            for (Endpoint endpoint : dueEndpoints) {
                // Check if we're at capacity
                if (activeChecks.get() >= maxConcurrentChecks) {
                    log.warn("Max concurrent checks reached ({}), skipping remaining endpoints",
                            maxConcurrentChecks);
                    break;
                }

                // Execute check asynchronously
                executeCheckAsync(endpoint);
            }

        } catch (Exception e) {
            log.error("Error in monitoring scheduler", e);
        }
    }

    /**
     * Execute a single endpoint check asynchronously
     */
    private void executeCheckAsync(Endpoint endpoint) {
        activeChecks.incrementAndGet();

        CompletableFuture.runAsync(() -> {
            try {
                monitoringService.executeCheck(endpoint).join();
            } catch (Exception e) {
                log.error("Check failed for endpoint {}: {}", 
                         endpoint.getId(), e.getMessage());
            } finally {
                activeChecks.decrementAndGet();
            }
        }, monitoringExecutor);
    }

    /**
     * Get current number of active checks (for monitoring)
     */
    public int getActiveCheckCount() {
        return activeChecks.get();
    }
}
