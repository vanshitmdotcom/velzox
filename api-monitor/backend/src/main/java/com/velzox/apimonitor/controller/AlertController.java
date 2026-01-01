package com.velzox.apimonitor.controller;

import com.velzox.apimonitor.dto.AlertDto;
import com.velzox.apimonitor.dto.ApiResponse;
import com.velzox.apimonitor.repository.AlertRepository;
import com.velzox.apimonitor.service.AlertService;
import com.velzox.apimonitor.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Alert Controller - Manages alert history and acknowledgment
 * 
 * ENDPOINTS:
 * - GET /api/v1/alerts - List alerts for the user
 * - GET /api/v1/alerts/unacknowledged - Get unacknowledged alerts
 * - POST /api/v1/alerts/{id}/acknowledge - Acknowledge an alert
 * - POST /api/v1/alerts/acknowledge-all - Acknowledge all alerts for an endpoint
 */
@RestController
@RequestMapping("/v1/alerts")
@RequiredArgsConstructor
@Slf4j
public class AlertController {

    private final AlertService alertService;
    private final AlertRepository alertRepository;
    private final UserService userService;

    /**
     * Get paginated list of alerts for the user
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AlertDto.Response>>> getAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<AlertDto.Response> alerts = alertRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageRequest)
                .map(AlertDto.Response::from);
        
        return ResponseEntity.ok(ApiResponse.success(alerts.getContent()));
    }

    /**
     * Get unacknowledged alerts for the user
     */
    @GetMapping("/unacknowledged")
    public ResponseEntity<ApiResponse<List<AlertDto.ListItem>>> getUnacknowledgedAlerts(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        
        List<AlertDto.ListItem> alerts = alertRepository
                .findByUserIdAndAcknowledgedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(AlertDto.ListItem::from)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    /**
     * Acknowledge a specific alert
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<ApiResponse<Void>> acknowledgeAlert(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        alertService.acknowledgeAlert(id, userId);
        
        return ResponseEntity.ok(ApiResponse.success("Alert acknowledged"));
    }

    /**
     * Acknowledge all alerts for an endpoint
     */
    @PostMapping("/acknowledge-all")
    public ResponseEntity<ApiResponse<Void>> acknowledgeAllForEndpoint(
            @RequestParam Long endpointId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = userService.getUserIdByEmail(userDetails.getUsername());
        alertService.acknowledgeAllForEndpoint(endpointId, userId);
        
        return ResponseEntity.ok(ApiResponse.success("All alerts acknowledged"));
    }
}
