package com.velzox.apimonitor.repository;

import com.velzox.apimonitor.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Project entity operations
 * 
 * Provides methods for:
 * - Fetching projects by owner
 * - Project statistics
 * - Endpoint aggregation
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /**
     * Find all projects owned by a user
     */
    List<Project> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    /**
     * Find project by ID and owner (for access control)
     */
    Optional<Project> findByIdAndOwnerId(Long id, Long ownerId);

    /**
     * Find project with endpoints eagerly loaded
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.endpoints WHERE p.id = :projectId")
    Optional<Project> findByIdWithEndpoints(Long projectId);

    /**
     * Find project with credentials eagerly loaded
     */
    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.credentials WHERE p.id = :projectId")
    Optional<Project> findByIdWithCredentials(Long projectId);

    /**
     * Count endpoints in a project
     */
    @Query("SELECT COUNT(e) FROM Endpoint e WHERE e.project.id = :projectId")
    long countEndpointsByProjectId(Long projectId);

    /**
     * Check if project name exists for owner
     */
    boolean existsByNameAndOwnerId(String name, Long ownerId);

    /**
     * Get project IDs for an owner (lightweight query)
     */
    @Query("SELECT p.id FROM Project p WHERE p.owner.id = :ownerId")
    List<Long> findProjectIdsByOwnerId(Long ownerId);
}
