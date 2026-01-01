package com.velzox.apimonitor.controller;

import com.velzox.apimonitor.dto.ApiResponse;
import com.velzox.apimonitor.dto.ProjectDto;
import com.velzox.apimonitor.service.ProjectService;
import com.velzox.apimonitor.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Project Controller - Manages projects that group endpoints
 * 
 * ENDPOINTS:
 * - GET /api/v1/projects - List all projects
 * - GET /api/v1/projects/{id} - Get project details
 * - POST /api/v1/projects - Create a new project
 * - PUT /api/v1/projects/{id} - Update a project
 * - DELETE /api/v1/projects/{id} - Delete a project
 */
@RestController
@RequestMapping("/v1/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;
    private final UserService userService;

    /**
     * Get all projects for the authenticated user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProjectDto.ListItem>>> getProjects(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        List<ProjectDto.ListItem> projects = projectService.getProjectsForUser(userId);
        
        return ResponseEntity.ok(ApiResponse.success(projects));
    }

    /**
     * Get a specific project by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDto.Response>> getProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        ProjectDto.Response project = projectService.getProject(id, userId);
        
        return ResponseEntity.ok(ApiResponse.success(project));
    }

    /**
     * Create a new project
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ProjectDto.Response>> createProject(
            @Valid @RequestBody ProjectDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        ProjectDto.Response project = projectService.createProject(request, userId);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Project created", project));
    }

    /**
     * Update an existing project
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectDto.Response>> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody ProjectDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        ProjectDto.Response project = projectService.updateProject(id, request, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Project updated", project));
    }

    /**
     * Delete a project and all its endpoints
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProject(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        projectService.deleteProject(id, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Project deleted"));
    }
}
