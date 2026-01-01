package com.velzox.apimonitor.dto;

import com.velzox.apimonitor.entity.Credential;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Credential DTOs - Request and response objects for credential management
 * 
 * SECURITY NOTES:
 * - Credential values are NEVER returned in responses
 * - Only masked versions (****xxxx) are shown
 * - Raw values are only used in memory during HTTP calls
 */
public class CredentialDto {

    /**
     * Create Credential Request
     * 
     * For different types:
     * - BEARER_TOKEN: Only value is required
     * - API_KEY: value and headerName are required
     * - BASIC_AUTH: value (password) and username are required
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "Credential name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;
        
        @NotNull(message = "Credential type is required")
        private Credential.CredentialType type;
        
        @NotBlank(message = "Credential value is required")
        private String value;
        
        /**
         * For API_KEY type: the header name
         * Example: "X-API-Key", "Authorization"
         */
        private String headerName;
        
        /**
         * For BASIC_AUTH type: the username
         */
        private String username;
        
        @Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;
        
        @NotNull(message = "Project ID is required")
        private Long projectId;
    }

    /**
     * Update Credential Request
     * 
     * Note: Value is optional - if not provided, existing value is kept
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String name;
        
        /**
         * New value - only update if provided
         */
        private String value;
        
        private String headerName;
        
        private String username;
        
        @Size(max = 500, message = "Description must not exceed 500 characters")
        private String description;
    }

    /**
     * Credential Response - NEVER includes raw credential values
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String name;
        private Credential.CredentialType type;
        
        /**
         * Masked value - shows only last 4 characters
         * Example: "****ab12"
         */
        private String maskedValue;
        
        private String headerName;
        
        /**
         * Masked username for BASIC_AUTH
         */
        private String maskedUsername;
        
        private String description;
        private Long projectId;
        private boolean inUse;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        /**
         * Create response from entity with masked values
         */
        public static Response from(Credential credential, String maskedValue, 
                                    String maskedUsername, boolean inUse) {
            return Response.builder()
                    .id(credential.getId())
                    .name(credential.getName())
                    .type(credential.getType())
                    .maskedValue(maskedValue)
                    .headerName(credential.getHeaderName())
                    .maskedUsername(maskedUsername)
                    .description(credential.getDescription())
                    .projectId(credential.getProject().getId())
                    .inUse(inUse)
                    .createdAt(credential.getCreatedAt())
                    .updatedAt(credential.getUpdatedAt())
                    .build();
        }
    }

    /**
     * Credential List Item - For dropdown selections
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private Long id;
        private String name;
        private Credential.CredentialType type;
        private boolean inUse;

        public static ListItem from(Credential credential, boolean inUse) {
            return ListItem.builder()
                    .id(credential.getId())
                    .name(credential.getName())
                    .type(credential.getType())
                    .inUse(inUse)
                    .build();
        }
    }

    /**
     * Convert list of credentials to list items
     */
    public static List<ListItem> toListItems(List<Credential> credentials, 
                                              List<Long> inUseIds) {
        return credentials.stream()
                .map(c -> ListItem.from(c, inUseIds.contains(c.getId())))
                .collect(Collectors.toList());
    }
}
