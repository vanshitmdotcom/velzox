package com.velzox.apimonitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Credential Entity - Secure storage for API authentication credentials
 * 
 * SECURITY DESIGN:
 * - Credentials are encrypted at rest using AES-256
 * - Only decrypted in memory when making HTTP requests
 * - Masked in all API responses (only last 4 characters shown)
 * - Never stored in logs or check results
 * 
 * SUPPORTED TYPES:
 * - BEARER_TOKEN: Authorization: Bearer <token>
 * - API_KEY: Custom header with API key
 * - BASIC_AUTH: Base64 encoded username:password
 * 
 * NOTE: This is for SERVICE/HEALTH tokens, not user login tokens.
 * We do NOT support OAuth refresh flows or session-based auth.
 */
@Entity
@Table(name = "credentials")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name for the credential
     * Example: "Production API Key", "Health Check Token"
     */
    @Column(nullable = false)
    private String name;

    /**
     * Type of credential (determines how it's used in requests)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CredentialType type;

    /**
     * ENCRYPTED credential value
     * This field stores AES-256 encrypted data
     * NEVER log or expose this value
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String encryptedValue;

    /**
     * For API_KEY type: the header name to use
     * Example: "X-API-Key", "Authorization"
     */
    private String headerName;

    /**
     * For BASIC_AUTH type: encrypted username
     */
    @Column(columnDefinition = "TEXT")
    private String encryptedUsername;

    /**
     * Project this credential belongs to
     * Credentials are scoped to projects for organization
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * Optional description for the credential
     */
    @Column(length = 500)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Supported credential types
     */
    public enum CredentialType {
        /**
         * Bearer token authentication
         * Adds header: Authorization: Bearer <token>
         */
        BEARER_TOKEN,

        /**
         * API Key authentication
         * Adds custom header: <headerName>: <key>
         * Common headers: X-API-Key, Api-Key, Authorization
         */
        API_KEY,

        /**
         * HTTP Basic Authentication
         * Adds header: Authorization: Basic <base64(username:password)>
         */
        BASIC_AUTH
    }

    /**
     * Get a masked representation of the credential for display
     * Shows only the last 4 characters: "****abcd"
     * 
     * @param decryptedValue The decrypted credential value
     * @return Masked string safe for display
     */
    public static String maskCredential(String decryptedValue) {
        if (decryptedValue == null || decryptedValue.length() <= 4) {
            return "****";
        }
        return "****" + decryptedValue.substring(decryptedValue.length() - 4);
    }
}
