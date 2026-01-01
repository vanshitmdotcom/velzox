package com.velzox.apimonitor.dto;

import com.velzox.apimonitor.entity.Alert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Alert DTOs - Response objects for alert endpoints
 */
public class AlertDto {

    /**
     * Alert Response - Full alert details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long endpointId;
        private String endpointName;
        private String endpointUrl;
        private Alert.AlertType type;
        private Alert.AlertSeverity severity;
        private String title;
        private String message;
        private Alert.AlertChannel channel;
        private boolean delivered;
        private String deliveryError;
        private boolean acknowledged;
        private LocalDateTime acknowledgedAt;
        private Long incidentId;
        private LocalDateTime createdAt;

        public static Response from(Alert alert) {
            return Response.builder()
                    .id(alert.getId())
                    .endpointId(alert.getEndpoint().getId())
                    .endpointName(alert.getEndpoint().getName())
                    .endpointUrl(alert.getEndpoint().getUrl())
                    .type(alert.getType())
                    .severity(alert.getSeverity())
                    .title(alert.getTitle())
                    .message(alert.getMessage())
                    .channel(alert.getChannel())
                    .delivered(alert.isDelivered())
                    .deliveryError(alert.getDeliveryError())
                    .acknowledged(alert.isAcknowledged())
                    .acknowledgedAt(alert.getAcknowledgedAt())
                    .incidentId(alert.getIncidentId())
                    .createdAt(alert.getCreatedAt())
                    .build();
        }
    }

    /**
     * Alert List Item - Lightweight for notifications panel
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListItem {
        private Long id;
        private String endpointName;
        private Alert.AlertType type;
        private Alert.AlertSeverity severity;
        private String title;
        private boolean acknowledged;
        private LocalDateTime createdAt;

        public static ListItem from(Alert alert) {
            return ListItem.builder()
                    .id(alert.getId())
                    .endpointName(alert.getEndpoint().getName())
                    .type(alert.getType())
                    .severity(alert.getSeverity())
                    .title(alert.getTitle())
                    .acknowledged(alert.isAcknowledged())
                    .createdAt(alert.getCreatedAt())
                    .build();
        }
    }

    /**
     * Alert Statistics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlertStats {
        private long totalAlerts;
        private long unacknowledgedCount;
        private long criticalCount;
        private long errorCount;
        private long warningCount;
        private long infoCount;
    }

    /**
     * Convert list of alerts to responses
     */
    public static List<Response> toResponseList(List<Alert> alerts) {
        return alerts.stream()
                .map(Response::from)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of alerts to list items
     */
    public static List<ListItem> toListItems(List<Alert> alerts) {
        return alerts.stream()
                .map(ListItem::from)
                .collect(Collectors.toList());
    }
}
