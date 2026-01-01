package com.velzox.apimonitor.service;

import com.velzox.apimonitor.dto.ProjectDto;
import com.velzox.apimonitor.entity.Endpoint;
import com.velzox.apimonitor.entity.Project;
import com.velzox.apimonitor.entity.User;
import com.velzox.apimonitor.exception.DuplicateResourceException;
import com.velzox.apimonitor.exception.ResourceNotFoundException;
import com.velzox.apimonitor.repository.EndpointRepository;
import com.velzox.apimonitor.repository.IncidentRepository;
import com.velzox.apimonitor.repository.ProjectRepository;
import com.velzox.apimonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Project Service - Manages projects that group endpoints together
 * 
 * FEATURES:
 * - Create, update, delete projects
 * - Project statistics and status aggregation
 * - Access control (users can only access their own projects)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final EndpointRepository endpointRepository;
    private final IncidentRepository incidentRepository;

    /**
     * Get all projects for a user
     * 
     * @param userId User's ID
     * @return List of projects with status counts
     */
    @Transactional(readOnly = true)
    public List<ProjectDto.ListItem> getProjectsForUser(Long userId) {
        log.debug("Fetching projects for user: {}", userId);

        List<Project> projects = projectRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
        
        return projects.stream()
                .map(project -> {
                    // Count endpoints by status
                    Map<Endpoint.EndpointStatus, Long> statusCounts = countEndpointsByStatus(project.getId());
                    int upCount = statusCounts.getOrDefault(Endpoint.EndpointStatus.UP, 0L).intValue();
                    int downCount = statusCounts.getOrDefault(Endpoint.EndpointStatus.DOWN, 0L).intValue();
                    
                    return ProjectDto.ListItem.from(project, upCount, downCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * Get a specific project by ID
     * 
     * @param projectId Project ID
     * @param userId User ID (for access control)
     * @return Project details with statistics
     */
    @Transactional(readOnly = true)
    public ProjectDto.Response getProject(Long projectId, Long userId) {
        log.debug("Fetching project {} for user {}", projectId, userId);

        Project project = projectRepository.findByIdAndOwnerId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        // Calculate statistics
        ProjectDto.ProjectStats stats = calculateProjectStats(projectId, userId);

        return ProjectDto.Response.fromWithStats(project, stats);
    }

    /**
     * Create a new project
     * 
     * @param request Project creation data
     * @param userId Owner's user ID
     * @return Created project
     */
    @Transactional
    public ProjectDto.Response createProject(ProjectDto.CreateRequest request, Long userId) {
        log.info("Creating project '{}' for user {}", request.getName(), userId);

        // Check for duplicate name
        if (projectRepository.existsByNameAndOwnerId(request.getName(), userId)) {
            throw DuplicateResourceException.projectName(request.getName());
        }

        // Get user
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        // Create project
        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .active(true)
                .build();

        project = projectRepository.save(project);
        log.info("Project created: {}", project.getId());

        return ProjectDto.Response.from(project);
    }

    /**
     * Update an existing project
     * 
     * @param projectId Project ID
     * @param request Update data
     * @param userId User ID (for access control)
     * @return Updated project
     */
    @Transactional
    public ProjectDto.Response updateProject(Long projectId, ProjectDto.UpdateRequest request, 
                                             Long userId) {
        log.info("Updating project {} for user {}", projectId, userId);

        Project project = projectRepository.findByIdAndOwnerId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        // Check for duplicate name (if changed)
        if (!project.getName().equals(request.getName()) &&
            projectRepository.existsByNameAndOwnerId(request.getName(), userId)) {
            throw DuplicateResourceException.projectName(request.getName());
        }

        // Update fields
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        if (request.getActive() != null) {
            project.setActive(request.getActive());
        }

        project = projectRepository.save(project);
        log.info("Project updated: {}", projectId);

        return ProjectDto.Response.from(project);
    }

    /**
     * Delete a project and all its endpoints
     * 
     * @param projectId Project ID
     * @param userId User ID (for access control)
     */
    @Transactional
    public void deleteProject(Long projectId, Long userId) {
        log.info("Deleting project {} for user {}", projectId, userId);

        Project project = projectRepository.findByIdAndOwnerId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        projectRepository.delete(project);
        log.info("Project deleted: {}", projectId);
    }

    /**
     * Calculate statistics for a project
     */
    private ProjectDto.ProjectStats calculateProjectStats(Long projectId, Long userId) {
        Map<Endpoint.EndpointStatus, Long> statusCounts = countEndpointsByStatus(projectId);
        
        long openIncidents = incidentRepository.countOpenIncidentsByUserId(userId);

        return ProjectDto.ProjectStats.builder()
                .totalEndpoints(statusCounts.values().stream().mapToInt(Long::intValue).sum())
                .upEndpoints(statusCounts.getOrDefault(Endpoint.EndpointStatus.UP, 0L).intValue())
                .downEndpoints(statusCounts.getOrDefault(Endpoint.EndpointStatus.DOWN, 0L).intValue())
                .degradedEndpoints(statusCounts.getOrDefault(Endpoint.EndpointStatus.DEGRADED, 0L).intValue())
                .openIncidents((int) openIncidents)
                .build();
    }

    /**
     * Count endpoints by status for a project
     */
    private Map<Endpoint.EndpointStatus, Long> countEndpointsByStatus(Long projectId) {
        List<Object[]> results = endpointRepository.countByStatusForProject(projectId);
        Map<Endpoint.EndpointStatus, Long> counts = new HashMap<>();
        
        for (Object[] row : results) {
            Endpoint.EndpointStatus status = (Endpoint.EndpointStatus) row[0];
            Long count = (Long) row[1];
            counts.put(status, count);
        }
        
        return counts;
    }
}
