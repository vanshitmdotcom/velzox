package com.velzox.apimonitor.controller;

import com.velzox.apimonitor.dto.ApiResponse;
import com.velzox.apimonitor.dto.EndpointDto;
import com.velzox.apimonitor.service.EndpointService;
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
 * Endpoint Controller - Manages monitored API endpoints
 * 
 * ENDPOINTS:
 * - GET /api/v1/endpoints?projectId={id} - List endpoints for a project
 * - GET /api/v1/endpoints/{id} - Get endpoint details
 * - POST /api/v1/endpoints - Create a new endpoint
 * - PUT /api/v1/endpoints/{id} - Update an endpoint
 * - DELETE /api/v1/endpoints/{id} - Delete an endpoint
 * - PATCH /api/v1/endpoints/{id}/toggle - Enable/disable endpoint
 */
@RestController
@RequestMapping("/api/v1/endpoints")
@RequiredArgsConstructor
@Slf4j
public class EndpointController {

    private final EndpointService endpointService;
    private final UserService userService;

    /**
     * Get all endpoints for a project
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EndpointDto.ListItem>>> getEndpoints(
            @RequestParam Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        List<EndpointDto.ListItem> endpoints = endpointService.getEndpointsForProject(
                projectId, userId);
        
        return ResponseEntity.ok(ApiResponse.success(endpoints));
    }

    /**
     * Get a specific endpoint by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EndpointDto.Response>> getEndpoint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        EndpointDto.Response endpoint = endpointService.getEndpoint(id, userId);
        
        return ResponseEntity.ok(ApiResponse.success(endpoint));
    }

    /**
     * Create a new endpoint
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EndpointDto.Response>> createEndpoint(
            @Valid @RequestBody EndpointDto.CreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        EndpointDto.Response endpoint = endpointService.createEndpoint(request, userId);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Endpoint created", endpoint));
    }

    /**
     * Update an existing endpoint
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EndpointDto.Response>> updateEndpoint(
            @PathVariable Long id,
            @Valid @RequestBody EndpointDto.UpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        EndpointDto.Response endpoint = endpointService.updateEndpoint(id, request, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Endpoint updated", endpoint));
    }

    /**
     * Delete an endpoint
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEndpoint(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        endpointService.deleteEndpoint(id, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Endpoint deleted"));
    }

    /**
     * Toggle endpoint enabled/disabled status
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleEndpoint(
            @PathVariable Long id,
            @RequestParam boolean enabled,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        endpointService.toggleEndpoint(id, enabled, userId);
        
        String message = enabled ? "Endpoint enabled" : "Endpoint disabled";
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
