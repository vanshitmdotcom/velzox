package com.velzox.apimonitor.controller;

import com.velzox.apimonitor.dto.ApiResponse;
import com.velzox.apimonitor.scheduler.MonitoringScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Health Controller - Application health check endpoint
 * 
 * ENDPOINTS:
 * - GET /api/health - Basic health check (public)
 * - GET /api/health/detailed - Detailed health info (internal)
 * 
 * Used for:
 * - Load balancer health checks
 * - Kubernetes liveness/readiness probes
 * - Uptime monitoring
 */
@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final MonitoringScheduler monitoringScheduler;

    /**
     * Basic health check - returns 200 if application is running
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "API Monitor");
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    /**
     * Detailed health check - includes internal metrics
     */
    @GetMapping("/detailed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "API Monitor");
        health.put("version", "1.0.0");
        
        // Monitoring metrics
        Map<String, Object> monitoring = new HashMap<>();
        monitoring.put("activeChecks", monitoringScheduler.getActiveCheckCount());
        health.put("monitoring", monitoring);
        
        // Memory metrics
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        memory.put("totalMB", runtime.totalMemory() / 1024 / 1024);
        memory.put("freeMB", runtime.freeMemory() / 1024 / 1024);
        memory.put("usedMB", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
        memory.put("maxMB", runtime.maxMemory() / 1024 / 1024);
        health.put("memory", memory);
        
        return ResponseEntity.ok(ApiResponse.success(health));
    }
}
