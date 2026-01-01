package com.velzox.apimonitor.repository;

import com.velzox.apimonitor.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for User entity operations
 * 
 * Provides methods for:
 * - User authentication (findByEmail)
 * - Account management
 * - Statistics queries
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email address (for login)
     * @param email User's email
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email is already registered
     * @param email Email to check
     * @return true if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Count total endpoints across all projects for a user
     * Used to enforce plan limits
     */
    @Query("SELECT COUNT(e) FROM Endpoint e WHERE e.project.owner.id = :userId")
    long countEndpointsByUserId(Long userId);

    /**
     * Find user with projects eagerly loaded
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.projects WHERE u.id = :userId")
    Optional<User> findByIdWithProjects(Long userId);
}
