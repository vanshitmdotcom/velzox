package com.velzox.apimonitor.dto;

import com.velzox.apimonitor.entity.Endpoint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard DTOs - Response objects for dashboard views
 * 
 * Provides aggregated statistics and quick overview data
 * for the main monitoring dashboard.
 */
public class DashboardDto {

    /**
     * Main Dashboard Response - Overview of all monitoring data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {
        // Endpoint Summary
        private int totalEndpoints;
        private int upEndpoints;
        private int downEndpoints;
        private int degradedEndpoints;
        private int unknownEndpoints;
        
        // Overall Statistics
        private double overallUptimePercentage;
        private double avgLatencyMs;
        
        // Incident Summary
        private int openIncidents;
        private int resolvedIncidentsToday;
        
        // Alert Summary
        private int unacknowledgedAlerts;
        
        // Plan Usage
        private PlanUsage planUsage;
        
        // Recent Activity
        private List<EndpointStatus> endpointStatuses;
        private List<AlertDto.ListItem> recentAlerts;
        private List<IncidentSummary> openIncidentsList;
    }

    /**
     * Plan Usage Information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanUsage {
        private String planName;
        private int maxEndpoints;
        private int usedEndpoints;
        private int minCheckIntervalSeconds;
        private int historyDays;
        private boolean slackEnabled;
        private double usagePercentage;
    }

    /**
     * Endpoint Status for Dashboard Grid
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointStatus {
        private Long id;
        private String name;
        private String url;
        private String projectName;
        private Endpoint.EndpointStatus status;
        private double uptimePercentage;
        private double avgLatencyMs;
        private LocalDateTime lastCheckAt;
        private LocalDateTime lastFailureAt;
        private int consecutiveFailures;
    }

    /**
     * Incident Summary for Dashboard
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentSummary {
        private Long id;
        private String endpointName;
        private String endpointUrl;
        private String failureType;
        private LocalDateTime startedAt;
        private long durationMinutes;
        private int failedCheckCount;
        private String lastErrorMessage;
    }

    /**
     * Time Series Data Point for Charts
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesPoint {
        private LocalDateTime timestamp;
        private long totalChecks;
        private long successfulChecks;
        private double avgLatencyMs;
        private double uptimePercentage;
    }

    /**
     * Endpoint Performance Data for Charts
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointPerformance {
        private Long endpointId;
        private String endpointName;
        private List<TimeSeriesPoint> timeSeries;
        private LatencyStats latencyStats;
    }

    /**
     * Latency Statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LatencyStats {
        private double min;
        private double p50;
        private double p95;
        private double p99;
        private double max;
        private double avg;
    }

    /**
     * Uptime Report - For detailed uptime analysis
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UptimeReport {
        private Long endpointId;
        private String endpointName;
        private String url;
        
        // Overall uptime
        private double last24Hours;
        private double last7Days;
        private double last30Days;
        
        // Downtime details
        private long totalDowntimeMinutes;
        private int incidentCount;
        
        // Daily breakdown
        private List<DailyUptime> dailyBreakdown;
    }

    /**
     * Daily Uptime Data
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyUptime {
        private LocalDateTime date;
        private double uptimePercentage;
        private long downtimeMinutes;
        private int incidentCount;
    }
}
