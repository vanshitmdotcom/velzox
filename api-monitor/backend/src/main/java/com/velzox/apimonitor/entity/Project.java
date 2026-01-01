package com.velzox.apimonitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Project Entity - Groups related endpoints together
 * 
 * Projects allow users to:
 * - Organize endpoints by application/service
 * - Apply common settings across endpoints
 * - View aggregated statistics
 * 
 * Example: "E-commerce Backend", "Payment Gateway", "User Service"
 */
@Entity
@Table(name = "projects")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable project name
     */
    @Column(nullable = false)
    private String name;

    /**
     * Optional description explaining the project's purpose
     */
    @Column(length = 500)
    private String description;

    /**
     * Project owner - the user who created this project
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Endpoints being monitored within this project
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Endpoint> endpoints = new ArrayList<>();

    /**
     * Stored credentials for this project's endpoints
     * Credentials are encrypted at rest and shared across endpoints
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Credential> credentials = new ArrayList<>();

    /**
     * Whether the project is currently being monitored
     */
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Calculate total endpoint count for this project
     */
    public int getEndpointCount() {
        return endpoints != null ? endpoints.size() : 0;
    }
}
