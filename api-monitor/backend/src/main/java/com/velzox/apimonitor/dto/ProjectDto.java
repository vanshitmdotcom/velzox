package com.velzox.apimonitor.dto;

import com.velzox.apimonitor.entity.Project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Project DTOs - Request and response objects for project endpoints
 */
public class ProjectDto {

    /**
     * Create Project Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "Project name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;
        
        @Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;
    }

    /**
     * Update Project Request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @NotBlank(message = "Project name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;
        
        @Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;
        
        private Boolean active;
    }

    /**
     * Project Response - Full project details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private boolean active;
        private int endpointCount;
        private int credentialCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        // Statistics
        private ProjectStats stats;

        /**
         * Convert entity to response DTO
         */
        public static Response from(Project project) {
            return Response.builder()
                    .id(project.getId())
                    .name(project.getName())
                    .description(project.getDescription())
                    .active(project.isActive())
                    .endpointCount(project.getEndpoints() != null ? 
                                   project.getEndpoints().size() : 0)
                    .credentialCount(project.getCredentials() != null ? 
                                     project.getCredentials().size() : 0)
                    .createdAt(project.getCreatedAt())
                    .updatedAt(project.getUpdatedAt())
                    .build();
        }

        /**
         * Convert entity to response with statistics
         */
        public static Response fromWithStats(Project project, ProjectStats stats) {
            Response response = from(project);
            response.setStats(stats);
            return response;
        }
    }

    /**
     * Project List Item - Lightweight response for list views
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private Long id;
        private String name;
        private String description;
        private boolean active;
        private int endpointCount;
        private int upCount;
        private int downCount;
        private LocalDateTime createdAt;

        public static ListItem from(Project project, int upCount, int downCount) {
            return ListItem.builder()
                    .id(project.getId())
                    .name(project.getName())
                    .description(project.getDescription())
                    .active(project.isActive())
                    .endpointCount(project.getEndpoints() != null ? 
                                   project.getEndpoints().size() : 0)
                    .upCount(upCount)
                    .downCount(downCount)
                    .createdAt(project.getCreatedAt())
                    .build();
        }
    }

    /**
     * Project Statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectStats {
        private int totalEndpoints;
        private int upEndpoints;
        private int downEndpoints;
        private int degradedEndpoints;
        private double overallUptime;
        private double avgLatencyMs;
        private int openIncidents;
    }

    /**
     * Convert list of projects to response DTOs
     */
    public static List<Response> toResponseList(List<Project> projects) {
        return projects.stream()
                .map(Response::from)
                .collect(Collectors.toList());
    }
}
