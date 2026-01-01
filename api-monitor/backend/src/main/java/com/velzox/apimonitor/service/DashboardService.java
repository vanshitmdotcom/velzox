package com.velzox.apimonitor.service;

import com.velzox.apimonitor.dto.AlertDto;
import com.velzox.apimonitor.dto.DashboardDto;
import com.velzox.apimonitor.entity.Endpoint;
import com.velzox.apimonitor.entity.Incident;
import com.velzox.apimonitor.entity.User;
import com.velzox.apimonitor.exception.ResourceNotFoundException;
import com.velzox.apimonitor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dashboard Service - Provides aggregated data for the monitoring dashboard
 * 
 * FEATURES:
 * - Overview statistics (endpoints, uptime, incidents)
 * - Real-time endpoint status
 * - Recent alerts and incidents
 * - Plan usage information
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final UserRepository userRepository;
    private final EndpointRepository endpointRepository;
    private final CheckResultRepository checkResultRepository;
    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;
    private final ProjectRepository projectRepository;

    /**
     * Get the main dashboard overview
     * 
     * @param userId User's ID
     * @return Complete dashboard data
     */
    @Transactional(readOnly = true)
    public DashboardDto.Overview getDashboardOverview(Long userId) {
        log.debug("Getting dashboard overview for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24h = now.minusHours(24);

        // Get endpoint status counts
        Map<Endpoint.EndpointStatus, Long> statusCounts = getStatusCounts(userId);
        
        int totalEndpoints = statusCounts.values().stream()
                .mapToInt(Long::intValue).sum();
        int upEndpoints = statusCounts.getOrDefault(Endpoint.EndpointStatus.UP, 0L).intValue();
        int downEndpoints = statusCounts.getOrDefault(Endpoint.EndpointStatus.DOWN, 0L).intValue();
        int degradedEndpoints = statusCounts.getOrDefault(Endpoint.EndpointStatus.DEGRADED, 0L).intValue();
        int unknownEndpoints = statusCounts.getOrDefault(Endpoint.EndpointStatus.UNKNOWN, 0L).intValue();

        // Calculate overall uptime
        double overallUptime = calculateOverallUptime(userId, last24h);

        // Get incident counts
        long openIncidents = incidentRepository.countOpenIncidentsByUserId(userId);
        
        // Count resolved incidents today
        long resolvedToday = 0; // Simplified for MVP

        // Get unacknowledged alert count
        long unacknowledgedAlerts = alertRepository.countByUserIdAndAcknowledgedFalse(userId);

        // Get plan usage
        DashboardDto.PlanUsage planUsage = getPlanUsage(user);

        // Get endpoint statuses
        List<DashboardDto.EndpointStatus> endpointStatuses = getEndpointStatuses(userId, last24h);

        // Get recent alerts
        List<AlertDto.ListItem> recentAlerts = alertRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 5))
                .stream()
                .map(AlertDto.ListItem::from)
                .collect(Collectors.toList());

        // Get open incidents
        List<DashboardDto.IncidentSummary> openIncidentsList = getOpenIncidents(userId);

        return DashboardDto.Overview.builder()
                .totalEndpoints(totalEndpoints)
                .upEndpoints(upEndpoints)
                .downEndpoints(downEndpoints)
                .degradedEndpoints(degradedEndpoints)
                .unknownEndpoints(unknownEndpoints)
                .overallUptimePercentage(overallUptime)
                .avgLatencyMs(calculateAverageLatency(userId, last24h))
                .openIncidents((int) openIncidents)
                .resolvedIncidentsToday((int) resolvedToday)
                .unacknowledgedAlerts((int) unacknowledgedAlerts)
                .planUsage(planUsage)
                .endpointStatuses(endpointStatuses)
                .recentAlerts(recentAlerts)
                .openIncidentsList(openIncidentsList)
                .build();
    }

    /**
     * Get status counts for all endpoints
     */
    private Map<Endpoint.EndpointStatus, Long> getStatusCounts(Long userId) {
        List<Object[]> results = endpointRepository.countByStatusForUser(userId);
        Map<Endpoint.EndpointStatus, Long> counts = new HashMap<>();
        
        for (Object[] row : results) {
            Endpoint.EndpointStatus status = (Endpoint.EndpointStatus) row[0];
            Long count = (Long) row[1];
            counts.put(status, count);
        }
        
        return counts;
    }

    /**
     * Calculate overall uptime across all endpoints
     */
    private double calculateOverallUptime(Long userId, LocalDateTime since) {
        List<Long> projectIds = projectRepository.findProjectIdsByOwnerId(userId);
        if (projectIds.isEmpty()) {
            return 100.0;
        }

        double totalUptime = 0;
        int endpointCount = 0;

        for (Long projectId : projectIds) {
            List<Endpoint> endpoints = endpointRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
            for (Endpoint endpoint : endpoints) {
                Double uptime = checkResultRepository.calculateUptimePercentage(
                    endpoint.getId(), since);
                if (uptime != null) {
                    totalUptime += uptime;
                    endpointCount++;
                }
            }
        }

        return endpointCount > 0 ? totalUptime / endpointCount : 100.0;
    }

    /**
     * Calculate average latency across all endpoints
     */
    private double calculateAverageLatency(Long userId, LocalDateTime since) {
        List<Long> projectIds = projectRepository.findProjectIdsByOwnerId(userId);
        if (projectIds.isEmpty()) {
            return 0.0;
        }

        double totalLatency = 0;
        int count = 0;

        for (Long projectId : projectIds) {
            List<Endpoint> endpoints = endpointRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
            for (Endpoint endpoint : endpoints) {
                Double avgLatency = checkResultRepository.calculateAverageLatency(
                    endpoint.getId(), since);
                if (avgLatency != null) {
                    totalLatency += avgLatency;
                    count++;
                }
            }
        }

        return count > 0 ? totalLatency / count : 0.0;
    }

    /**
     * Get plan usage information
     */
    private DashboardDto.PlanUsage getPlanUsage(User user) {
        long usedEndpoints = userRepository.countEndpointsByUserId(user.getId());
        int maxEndpoints = user.getPlan().getMaxEndpoints();
        double usagePercentage = maxEndpoints > 0 ? 
                (double) usedEndpoints / maxEndpoints * 100 : 0;

        return DashboardDto.PlanUsage.builder()
                .planName(user.getPlan().name())
                .maxEndpoints(maxEndpoints)
                .usedEndpoints((int) usedEndpoints)
                .minCheckIntervalSeconds(user.getPlan().getMinCheckIntervalSeconds())
                .historyDays(user.getPlan().getHistoryDays())
                .slackEnabled(user.getPlan().isSlackEnabled())
                .usagePercentage(usagePercentage)
                .build();
    }

    /**
     * Get endpoint statuses for dashboard grid
     */
    private List<DashboardDto.EndpointStatus> getEndpointStatuses(Long userId, LocalDateTime since) {
        List<Long> projectIds = projectRepository.findProjectIdsByOwnerId(userId);
        
        return projectIds.stream()
                .flatMap(projectId -> endpointRepository
                        .findByProjectIdOrderByCreatedAtDesc(projectId).stream())
                .map(endpoint -> {
                    Double uptime = checkResultRepository.calculateUptimePercentage(
                        endpoint.getId(), since);
                    Double avgLatency = checkResultRepository.calculateAverageLatency(
                        endpoint.getId(), since);
                    LocalDateTime lastFailure = checkResultRepository.findLastFailureTime(
                        endpoint.getId());

                    return DashboardDto.EndpointStatus.builder()
                            .id(endpoint.getId())
                            .name(endpoint.getName())
                            .url(endpoint.getUrl())
                            .projectName(endpoint.getProject().getName())
                            .status(endpoint.getStatus())
                            .uptimePercentage(uptime != null ? uptime : 100.0)
                            .avgLatencyMs(avgLatency != null ? avgLatency : 0.0)
                            .lastCheckAt(endpoint.getLastCheckAt())
                            .lastFailureAt(lastFailure)
                            .consecutiveFailures(endpoint.getConsecutiveFailures())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Get open incidents for dashboard
     */
    private List<DashboardDto.IncidentSummary> getOpenIncidents(Long userId) {
        List<Incident> incidents = incidentRepository.findOpenIncidentsByUserId(userId);

        return incidents.stream()
                .map(incident -> DashboardDto.IncidentSummary.builder()
                        .id(incident.getId())
                        .endpointName(incident.getEndpoint().getName())
                        .endpointUrl(incident.getEndpoint().getUrl())
                        .failureType(incident.getFailureType().name())
                        .startedAt(incident.getStartedAt())
                        .durationMinutes(incident.getDurationMinutes())
                        .failedCheckCount(incident.getFailedCheckCount())
                        .lastErrorMessage(incident.getLastErrorMessage())
                        .build())
                .collect(Collectors.toList());
    }
}
