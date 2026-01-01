package com.velzox.apimonitor.controller;

import com.velzox.apimonitor.dto.ApiResponse;
import com.velzox.apimonitor.dto.DashboardDto;
import com.velzox.apimonitor.service.DashboardService;
import com.velzox.apimonitor.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Dashboard Controller - Provides aggregated monitoring data
 * 
 * ENDPOINTS:
 * - GET /api/v1/dashboard - Get complete dashboard overview
 * 
 * The dashboard provides:
 * - Endpoint status summary (up/down/degraded)
 * - Overall uptime percentage
 * - Open incidents
 * - Recent alerts
 * - Plan usage
 */
@RestController
@RequestMapping("/v1/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    /**
     * Get the main dashboard overview
     * 
     * Returns comprehensive monitoring data including:
     * - Endpoint counts by status
     * - Overall uptime and latency
     * - Open incidents
     * - Recent alerts
     * - Plan usage
     */
    @GetMapping
    public ResponseEntity<ApiResponse<DashboardDto.Overview>> getDashboard(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        DashboardDto.Overview overview = dashboardService.getDashboardOverview(userId);
        
        return ResponseEntity.ok(ApiResponse.success(overview));
    }
}
