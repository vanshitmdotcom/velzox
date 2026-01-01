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
 * User Entity - Represents a registered user in the system
 * 
 * Users can:
 * - Create and manage projects
 * - Configure endpoints for monitoring
 * - Receive alerts via email/Slack
 * - Subscribe to different pricing plans (FREE, STARTER, PRO)
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User's email address - used for login and alert notifications
     * Must be unique across all users
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * BCrypt-hashed password for secure authentication
     */
    @Column(nullable = false)
    private String password;

    /**
     * Display name for the user
     */
    @Column(nullable = false)
    private String name;

    /**
     * Subscription plan determining feature limits
     * FREE: 2 endpoints, 5-min checks, 24h history
     * STARTER: 10 endpoints, 1-min checks, 7-day history
     * PRO: 50 endpoints, 30-sec checks, 30-day history
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Plan plan = Plan.FREE;

    /**
     * Whether the user's email has been verified
     */
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * Whether the user account is active
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Projects owned by this user
     * Cascade delete ensures projects are removed when user is deleted
     */
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    /**
     * Subscription plan enumeration with associated limits
     */
    public enum Plan {
        FREE(2, 300, 1, false),           // 2 endpoints, 5-min, 1 day
        STARTER(10, 60, 7, false),        // 10 endpoints, 1-min, 7 days
        PRO(50, 30, 30, true);            // 50 endpoints, 30-sec, 30 days

        private final int maxEndpoints;
        private final int minCheckIntervalSeconds;
        private final int historyDays;
        private final boolean slackEnabled;

        Plan(int maxEndpoints, int minCheckIntervalSeconds, int historyDays, boolean slackEnabled) {
            this.maxEndpoints = maxEndpoints;
            this.minCheckIntervalSeconds = minCheckIntervalSeconds;
            this.historyDays = historyDays;
            this.slackEnabled = slackEnabled;
        }

        public int getMaxEndpoints() { return maxEndpoints; }
        public int getMinCheckIntervalSeconds() { return minCheckIntervalSeconds; }
        public int getHistoryDays() { return historyDays; }
        public boolean isSlackEnabled() { return slackEnabled; }
    }
}
