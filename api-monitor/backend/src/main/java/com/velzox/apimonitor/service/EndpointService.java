package com.velzox.apimonitor.service;

import com.velzox.apimonitor.dto.EndpointDto;
import com.velzox.apimonitor.entity.Credential;
import com.velzox.apimonitor.entity.Endpoint;
import com.velzox.apimonitor.entity.Project;
import com.velzox.apimonitor.entity.User;
import com.velzox.apimonitor.exception.PlanLimitExceededException;
import com.velzox.apimonitor.exception.ResourceNotFoundException;
import com.velzox.apimonitor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Endpoint Service - Manages API endpoints for monitoring
 * 
 * FEATURES:
 * - Create, update, delete monitored endpoints
 * - Plan-based limit enforcement (max endpoints, min check interval)
 * - Endpoint statistics and uptime calculations
 * - Integration with monitoring scheduler
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EndpointService {

    private final EndpointRepository endpointRepository;
    private final ProjectRepository projectRepository;
    private final CredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final CheckResultRepository checkResultRepository;

    /**
     * Get all endpoints for a project
     * 
     * @param projectId Project ID
     * @param userId User ID (for access control)
     * @return List of endpoints with basic stats
     */
    @Transactional(readOnly = true)
    public List<EndpointDto.ListItem> getEndpointsForProject(Long projectId, Long userId) {
        log.debug("Fetching endpoints for project {}", projectId);

        // Verify project access
        verifyProjectAccess(projectId, userId);

        List<Endpoint> endpoints = endpointRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        return endpoints.stream()
                .map(endpoint -> {
                    Double uptime = checkResultRepository.calculateUptimePercentage(
                        endpoint.getId(), since);
                    Double avgLatency = checkResultRepository.calculateAverageLatency(
                        endpoint.getId(), since);
                    LocalDateTime lastFailure = checkResultRepository.findLastFailureTime(
                        endpoint.getId());
                    
                    return EndpointDto.ListItem.fromWithStats(
                        endpoint, 
                        uptime != null ? uptime : 100.0,
                        avgLatency,
                        lastFailure
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a specific endpoint
     * 
     * @param endpointId Endpoint ID
     * @param userId User ID (for access control)
     * @return Endpoint details with statistics
     */
    @Transactional(readOnly = true)
    public EndpointDto.Response getEndpoint(Long endpointId, Long userId) {
        log.debug("Fetching endpoint {}", endpointId);

        Endpoint endpoint = endpointRepository.findByIdAndOwnerId(endpointId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", endpointId));

        EndpointDto.EndpointStats stats = calculateEndpointStats(endpoint);
        return EndpointDto.Response.fromWithStats(endpoint, stats);
    }

    /**
     * Create a new endpoint
     * 
     * @param request Endpoint creation data
     * @param userId User ID (for access control and plan limits)
     * @return Created endpoint
     */
    @Transactional
    public EndpointDto.Response createEndpoint(EndpointDto.CreateRequest request, Long userId) {
        log.info("Creating endpoint '{}' in project {}", request.getName(), request.getProjectId());

        // Get user and verify plan limits
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Check endpoint limit
        long currentEndpoints = userRepository.countEndpointsByUserId(userId);
        int maxEndpoints = user.getPlan().getMaxEndpoints();
        if (currentEndpoints >= maxEndpoints) {
            throw PlanLimitExceededException.endpointLimit((int) currentEndpoints, maxEndpoints);
        }

        // Check check interval limit
        int minInterval = user.getPlan().getMinCheckIntervalSeconds();
        if (request.getCheckIntervalSeconds() < minInterval) {
            throw PlanLimitExceededException.checkIntervalLimit(
                request.getCheckIntervalSeconds(), minInterval);
        }

        // Verify project access
        Project project = verifyProjectAccess(request.getProjectId(), userId);

        // Get credential if specified
        Credential credential = null;
        if (request.getCredentialId() != null) {
            credential = credentialRepository.findByIdAndProjectId(
                request.getCredentialId(), request.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Credential", 
                                                                  request.getCredentialId()));
        }

        // Create endpoint
        Endpoint endpoint = Endpoint.builder()
                .name(request.getName())
                .url(request.getUrl())
                .method(request.getMethod())
                .headers(request.getHeaders())
                .requestBody(request.getRequestBody())
                .expectedStatusCode(request.getExpectedStatusCode())
                .checkIntervalSeconds(request.getCheckIntervalSeconds())
                .timeoutMs(request.getTimeoutMs())
                .maxLatencyMs(request.getMaxLatencyMs())
                .credential(credential)
                .project(project)
                .status(Endpoint.EndpointStatus.UNKNOWN)
                .enabled(true)
                .nextCheckAt(LocalDateTime.now())  // Schedule immediate check
                .build();

        endpoint = endpointRepository.save(endpoint);
        log.info("Endpoint created: {}", endpoint.getId());

        return EndpointDto.Response.from(endpoint);
    }

    /**
     * Update an existing endpoint
     * 
     * @param endpointId Endpoint ID
     * @param request Update data
     * @param userId User ID (for access control)
     * @return Updated endpoint
     */
    @Transactional
    public EndpointDto.Response updateEndpoint(Long endpointId, 
                                               EndpointDto.UpdateRequest request,
                                               Long userId) {
        log.info("Updating endpoint {}", endpointId);

        Endpoint endpoint = endpointRepository.findByIdAndOwnerId(endpointId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", endpointId));

        // Get user for plan limits
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Check check interval limit if being updated
        if (request.getCheckIntervalSeconds() != null) {
            int minInterval = user.getPlan().getMinCheckIntervalSeconds();
            if (request.getCheckIntervalSeconds() < minInterval) {
                throw PlanLimitExceededException.checkIntervalLimit(
                    request.getCheckIntervalSeconds(), minInterval);
            }
            endpoint.setCheckIntervalSeconds(request.getCheckIntervalSeconds());
        }

        // Update fields if provided
        if (request.getName() != null) {
            endpoint.setName(request.getName());
        }
        if (request.getUrl() != null) {
            endpoint.setUrl(request.getUrl());
        }
        if (request.getMethod() != null) {
            endpoint.setMethod(request.getMethod());
        }
        if (request.getHeaders() != null) {
            endpoint.setHeaders(request.getHeaders());
        }
        if (request.getRequestBody() != null) {
            endpoint.setRequestBody(request.getRequestBody());
        }
        if (request.getExpectedStatusCode() != null) {
            endpoint.setExpectedStatusCode(request.getExpectedStatusCode());
        }
        if (request.getTimeoutMs() != null) {
            endpoint.setTimeoutMs(request.getTimeoutMs());
        }
        if (request.getMaxLatencyMs() != null) {
            endpoint.setMaxLatencyMs(request.getMaxLatencyMs());
        }
        if (request.getEnabled() != null) {
            endpoint.setEnabled(request.getEnabled());
            // Reset status if re-enabled
            if (request.getEnabled()) {
                endpoint.setStatus(Endpoint.EndpointStatus.UNKNOWN);
                endpoint.setNextCheckAt(LocalDateTime.now());
            }
        }

        // Update credential if specified
        if (request.getCredentialId() != null) {
            if (request.getCredentialId() == 0) {
                // Remove credential
                endpoint.setCredential(null);
            } else {
                Credential credential = credentialRepository.findByIdAndProjectId(
                    request.getCredentialId(), endpoint.getProject().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Credential", 
                                                                      request.getCredentialId()));
                endpoint.setCredential(credential);
            }
        }

        endpoint = endpointRepository.save(endpoint);
        log.info("Endpoint updated: {}", endpointId);

        EndpointDto.EndpointStats stats = calculateEndpointStats(endpoint);
        return EndpointDto.Response.fromWithStats(endpoint, stats);
    }

    /**
     * Delete an endpoint
     * 
     * @param endpointId Endpoint ID
     * @param userId User ID (for access control)
     */
    @Transactional
    public void deleteEndpoint(Long endpointId, Long userId) {
        log.info("Deleting endpoint {}", endpointId);

        Endpoint endpoint = endpointRepository.findByIdAndOwnerId(endpointId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", endpointId));

        endpointRepository.delete(endpoint);
        log.info("Endpoint deleted: {}", endpointId);
    }

    /**
     * Toggle endpoint enabled/disabled status
     * 
     * @param endpointId Endpoint ID
     * @param enabled New status
     * @param userId User ID (for access control)
     */
    @Transactional
    public void toggleEndpoint(Long endpointId, boolean enabled, Long userId) {
        log.info("Toggling endpoint {} to {}", endpointId, enabled);

        Endpoint endpoint = endpointRepository.findByIdAndOwnerId(endpointId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint", endpointId));

        endpoint.setEnabled(enabled);
        if (enabled) {
            endpoint.setStatus(Endpoint.EndpointStatus.UNKNOWN);
            endpoint.setNextCheckAt(LocalDateTime.now());
        }

        endpointRepository.save(endpoint);
    }

    /**
     * Get all endpoints due for checking (for scheduler)
     */
    @Transactional(readOnly = true)
    public List<Endpoint> getEndpointsDueForCheck() {
        return endpointRepository.findEndpointsDueForCheck(LocalDateTime.now());
    }

    /**
     * Verify user has access to project
     */
    private Project verifyProjectAccess(Long projectId, Long userId) {
        return projectRepository.findByIdAndOwnerId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    /**
     * Calculate statistics for an endpoint
     */
    private EndpointDto.EndpointStats calculateEndpointStats(Endpoint endpoint) {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        
        Double uptime = checkResultRepository.calculateUptimePercentage(endpoint.getId(), since);
        Double avgLatency = checkResultRepository.calculateAverageLatency(endpoint.getId(), since);
        long totalChecks = checkResultRepository.countByEndpointIdAndCreatedAtAfter(
            endpoint.getId(), since);
        long successfulChecks = checkResultRepository.countByEndpointIdAndSuccessTrueAndCreatedAtAfter(
            endpoint.getId(), since);
        LocalDateTime lastFailure = checkResultRepository.findLastFailureTime(endpoint.getId());

        return EndpointDto.EndpointStats.builder()
                .uptimePercentage(uptime != null ? uptime : 100.0)
                .avgLatencyMs(avgLatency != null ? avgLatency : 0.0)
                .totalChecks(totalChecks)
                .successfulChecks(successfulChecks)
                .failedChecks(totalChecks - successfulChecks)
                .lastFailureAt(lastFailure)
                .build();
    }
}
