package com.velzox.apimonitor.dto;

import com.velzox.apimonitor.entity.Endpoint;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoint DTOs - Request and response objects for endpoint management
 */
public class EndpointDto {

    /**
     * Create Endpoint Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "Endpoint name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;
        
        @NotBlank(message = "URL is required")
        @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
        private String url;
        
        @NotNull(message = "HTTP method is required")
        private Endpoint.HttpMethod method;
        
        /**
         * Custom headers as JSON string
         * Example: {"Content-Type": "application/json"}
         */
        private String headers;
        
        /**
         * Request body for POST/PUT requests
         */
        private String requestBody;
        
        @Min(value = 100, message = "Expected status must be at least 100")
        @Max(value = 599, message = "Expected status must be at most 599")
        private int expectedStatusCode = 200;
        
        @Min(value = 30, message = "Check interval must be at least 30 seconds")
        @Max(value = 3600, message = "Check interval must be at most 1 hour")
        private int checkIntervalSeconds = 300;
        
        @Min(value = 1000, message = "Timeout must be at least 1 second")
        @Max(value = 60000, message = "Timeout must be at most 60 seconds")
        private int timeoutMs = 30000;
        
        /**
         * Optional latency threshold for alerts
         */
        @Min(value = 100, message = "Max latency must be at least 100ms")
        private Integer maxLatencyMs;
        
        /**
         * Reference to stored credential (by ID)
         */
        private Long credentialId;
        
        /**
         * Project to add the endpoint to
         */
        @NotNull(message = "Project ID is required")
        private Long projectId;
    }

    /**
     * Update Endpoint Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;
        
        @Pattern(regexp = "^https?://.*", message = "URL must start with http:// or https://")
        private String url;
        
        private Endpoint.HttpMethod method;
        
        private String headers;
        
        private String requestBody;
        
        @Min(value = 100, message = "Expected status must be at least 100")
        @Max(value = 599, message = "Expected status must be at most 599")
        private Integer expectedStatusCode;
        
        @Min(value = 30, message = "Check interval must be at least 30 seconds")
        @Max(value = 3600, message = "Check interval must be at most 1 hour")
        private Integer checkIntervalSeconds;
        
        @Min(value = 1000, message = "Timeout must be at least 1 second")
        @Max(value = 60000, message = "Timeout must be at most 60 seconds")
        private Integer timeoutMs;
        
        @Min(value = 100, message = "Max latency must be at least 100ms")
        private Integer maxLatencyMs;
        
        private Long credentialId;
        
        private Boolean enabled;
    }

    /**
     * Endpoint Response - Full endpoint details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String url;
        private Endpoint.HttpMethod method;
        private String headers;
        private String requestBody;
        private int expectedStatusCode;
        private int checkIntervalSeconds;
        private int timeoutMs;
        private Integer maxLatencyMs;
        private Long credentialId;
        private String credentialName;
        private Long projectId;
        private String projectName;
        private Endpoint.EndpointStatus status;
        private boolean enabled;
        private LocalDateTime lastCheckAt;
        private LocalDateTime nextCheckAt;
        private int consecutiveFailures;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        // Statistics
        private EndpointStats stats;

        public static Response from(Endpoint endpoint) {
            return Response.builder()
                    .id(endpoint.getId())
                    .name(endpoint.getName())
                    .url(endpoint.getUrl())
                    .method(endpoint.getMethod())
                    .headers(endpoint.getHeaders())
                    .requestBody(endpoint.getRequestBody())
                    .expectedStatusCode(endpoint.getExpectedStatusCode())
                    .checkIntervalSeconds(endpoint.getCheckIntervalSeconds())
                    .timeoutMs(endpoint.getTimeoutMs())
                    .maxLatencyMs(endpoint.getMaxLatencyMs())
                    .credentialId(endpoint.getCredential() != null ? 
                                  endpoint.getCredential().getId() : null)
                    .credentialName(endpoint.getCredential() != null ? 
                                    endpoint.getCredential().getName() : null)
                    .projectId(endpoint.getProject().getId())
                    .projectName(endpoint.getProject().getName())
                    .status(endpoint.getStatus())
                    .enabled(endpoint.isEnabled())
                    .lastCheckAt(endpoint.getLastCheckAt())
                    .nextCheckAt(endpoint.getNextCheckAt())
                    .consecutiveFailures(endpoint.getConsecutiveFailures())
                    .createdAt(endpoint.getCreatedAt())
                    .updatedAt(endpoint.getUpdatedAt())
                    .build();
        }

        public static Response fromWithStats(Endpoint endpoint, EndpointStats stats) {
            Response response = from(endpoint);
            response.setStats(stats);
            return response;
        }
    }

    /**
     * Endpoint List Item - Lightweight for dashboard/list views
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private Long id;
        private String name;
        private String url;
        private Endpoint.HttpMethod method;
        private Endpoint.EndpointStatus status;
        private boolean enabled;
        private LocalDateTime lastCheckAt;
        private Double uptimePercentage;
        private Double avgLatencyMs;
        private LocalDateTime lastFailureAt;

        public static ListItem from(Endpoint endpoint) {
            return ListItem.builder()
                    .id(endpoint.getId())
                    .name(endpoint.getName())
                    .url(endpoint.getUrl())
                    .method(endpoint.getMethod())
                    .status(endpoint.getStatus())
                    .enabled(endpoint.isEnabled())
                    .lastCheckAt(endpoint.getLastCheckAt())
                    .build();
        }

        public static ListItem fromWithStats(Endpoint endpoint, Double uptime, 
                                              Double avgLatency, LocalDateTime lastFailure) {
            ListItem item = from(endpoint);
            item.setUptimePercentage(uptime);
            item.setAvgLatencyMs(avgLatency);
            item.setLastFailureAt(lastFailure);
            return item;
        }
    }

    /**
     * Endpoint Statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndpointStats {
        private double uptimePercentage;
        private double avgLatencyMs;
        private long totalChecks;
        private long successfulChecks;
        private long failedChecks;
        private LocalDateTime lastFailureAt;
        private Long currentIncidentId;
    }

    /**
     * Convert list of endpoints to list items
     */
    public static List<ListItem> toListItems(List<Endpoint> endpoints) {
        return endpoints.stream()
                .map(ListItem::from)
                .collect(Collectors.toList());
    }
}
