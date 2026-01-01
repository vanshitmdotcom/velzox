package com.velzox.apimonitor.repository;

import com.velzox.apimonitor.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Credential entity operations
 * 
 * Provides methods for:
 * - Managing encrypted credentials
 * - Project-scoped credential access
 * 
 * SECURITY NOTE: Credential values are always encrypted.
 * This repository handles encrypted data only.
 */
@Repository
public interface CredentialRepository extends JpaRepository<Credential, Long> {

    /**
     * Find all credentials for a project
     */
    List<Credential> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    /**
     * Find credential by ID and verify project ownership
     */
    @Query("SELECT c FROM Credential c WHERE c.id = :credentialId " +
           "AND c.project.owner.id = :ownerId")
    Optional<Credential> findByIdAndOwnerId(Long credentialId, Long ownerId);

    /**
     * Find credential by ID and project
     */
    Optional<Credential> findByIdAndProjectId(Long id, Long projectId);

    /**
     * Check if credential name exists in project
     */
    boolean existsByNameAndProjectId(String name, Long projectId);

    /**
     * Count credentials by project
     */
    long countByProjectId(Long projectId);

    /**
     * Check if credential is in use by any endpoint
     */
    @Query("SELECT COUNT(e) > 0 FROM Endpoint e WHERE e.credential.id = :credentialId")
    boolean isCredentialInUse(Long credentialId);

    /**
     * Get credential IDs used by endpoints in a project
     */
    @Query("SELECT DISTINCT e.credential.id FROM Endpoint e " +
           "WHERE e.project.id = :projectId AND e.credential IS NOT NULL")
    List<Long> findCredentialIdsInUseByProject(Long projectId);
}
