package com.velzox.apimonitor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velzox.apimonitor.entity.CheckResult;
import com.velzox.apimonitor.entity.Endpoint;
import com.velzox.apimonitor.entity.Incident;
import com.velzox.apimonitor.repository.CheckResultRepository;
import com.velzox.apimonitor.repository.EndpointRepository;
import com.velzox.apimonitor.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Monitoring Service - Core engine for API endpoint monitoring
 * 
 * RESPONSIBILITIES:
 * - Execute HTTP checks against monitored endpoints
 * - Measure response time (latency)
 * - Classify check results (success, timeout, error, etc.)
 * - Store check results in database
 * - Trigger alerts for failures
 * - Manage incidents (open on failure, close on recovery)
 * 
 * PERFORMANCE:
 * - Uses async WebClient for non-blocking I/O
 * - Each check runs in its own thread from the pool
 * - Timeouts are enforced per-endpoint
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MonitoringService {

    private final WebClient webClient;
    private final EndpointRepository endpointRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;
    private final CredentialService credentialService;
    private final AlertService alertService;
    private final ObjectMapper objectMapper;

    /**
     * Execute a health check for an endpoint
     * 
     * This is the main entry point for monitoring.
     * Called by the scheduler for each endpoint due for a check.
     * 
     * @param endpoint The endpoint to check
     * @return CompletableFuture containing the check result
     */
    public CompletableFuture<CheckResult> executeCheck(Endpoint endpoint) {
        log.debug("Executing check for endpoint: {} ({})", endpoint.getName(), endpoint.getUrl());

        long startTime = System.currentTimeMillis();

        return buildRequest(endpoint)
                .exchangeToMono(response -> {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    int statusCode = response.statusCode().value();
                    
                    // Classify the result
                    CheckResult.CheckResultType resultType = CheckResult.classifyResult(
                            statusCode,
                            endpoint.getExpectedStatusCode(),
                            latencyMs,
                            endpoint.getMaxLatencyMs(),
                            null
                    );

                    // Create check result
                    CheckResult result;
                    if (resultType == CheckResult.CheckResultType.SUCCESS) {
                        result = CheckResult.success(endpoint, statusCode, latencyMs);
                    } else {
                        String errorMsg = generateErrorMessage(resultType, statusCode, 
                                                               endpoint.getExpectedStatusCode());
                        result = CheckResult.failure(endpoint, resultType, statusCode, 
                                                     latencyMs, errorMsg);
                    }

                    return Mono.just(result);
                })
                .timeout(Duration.ofMillis(endpoint.getTimeoutMs()))
                .onErrorResume(error -> {
                    long latencyMs = System.currentTimeMillis() - startTime;
                    return Mono.just(handleCheckError(endpoint, error, latencyMs));
                })
                .toFuture()
                .thenApply(result -> processCheckResult(endpoint, result));
    }

    /**
     * Build the HTTP request based on endpoint configuration
     */
    private WebClient.RequestHeadersSpec<?> buildRequest(Endpoint endpoint) {
        HttpMethod method = HttpMethod.valueOf(endpoint.getMethod().name());
        
        WebClient.RequestBodySpec requestSpec = webClient
                .method(method)
                .uri(endpoint.getUrl())
                .accept(MediaType.APPLICATION_JSON);

        // Add custom headers
        if (endpoint.getHeaders() != null && !endpoint.getHeaders().isEmpty()) {
            try {
                Map<String, String> headers = objectMapper.readValue(
                        endpoint.getHeaders(), new TypeReference<Map<String, String>>() {});
                headers.forEach(requestSpec::header);
            } catch (Exception e) {
                log.warn("Failed to parse headers for endpoint {}: {}", 
                        endpoint.getId(), e.getMessage());
            }
        }

        // Add authentication if credential is configured
        if (endpoint.getCredential() != null) {
            CredentialService.DecryptedCredential cred = 
                    credentialService.getDecryptedCredential(endpoint.getCredential().getId());
            requestSpec.header(cred.getHeaderNameToUse(), cred.getAuthorizationHeader());
        }

        // Add request body for POST/PUT/PATCH
        if (endpoint.getRequestBody() != null && !endpoint.getRequestBody().isEmpty() &&
            (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH)) {
            return requestSpec
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(endpoint.getRequestBody());
        }

        return requestSpec;
    }

    /**
     * Handle errors that occur during the check
     */
    private CheckResult handleCheckError(Endpoint endpoint, Throwable error, long latencyMs) {
        log.debug("Check error for endpoint {}: {}", endpoint.getId(), error.getMessage());

        CheckResult.CheckResultType resultType;
        String errorMessage;
        int statusCode = 0;

        if (error instanceof WebClientResponseException responseError) {
            statusCode = responseError.getStatusCode().value();
            resultType = CheckResult.classifyResult(statusCode, endpoint.getExpectedStatusCode(),
                                                    latencyMs, endpoint.getMaxLatencyMs(), null);
            errorMessage = "HTTP " + statusCode + ": " + responseError.getStatusText();
        } else if (error.getMessage() != null && error.getMessage().contains("timeout")) {
            resultType = CheckResult.CheckResultType.TIMEOUT;
            errorMessage = "Request timed out after " + endpoint.getTimeoutMs() + "ms";
        } else if (error.getMessage() != null && 
                   (error.getMessage().contains("SSL") || error.getMessage().contains("certificate"))) {
            resultType = CheckResult.CheckResultType.SSL_ERROR;
            errorMessage = "SSL/TLS error: " + error.getMessage();
        } else if (error.getMessage() != null && 
                   (error.getMessage().contains("Connection") || error.getMessage().contains("refused"))) {
            resultType = CheckResult.CheckResultType.CONNECTION_ERROR;
            errorMessage = "Connection failed: " + error.getMessage();
        } else {
            resultType = CheckResult.CheckResultType.UNKNOWN_ERROR;
            errorMessage = "Unexpected error: " + error.getMessage();
        }

        return CheckResult.failure(endpoint, resultType, statusCode, latencyMs, errorMessage);
    }

    /**
     * Process the check result - save to DB, update endpoint, trigger alerts
     */
    @Transactional
    public CheckResult processCheckResult(Endpoint endpoint, CheckResult result) {
        log.debug("Processing check result for endpoint {}: {}", 
                 endpoint.getId(), result.getResultType());

        // Save check result
        result = checkResultRepository.save(result);

        // Update endpoint status
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextCheck = now.plusSeconds(endpoint.getCheckIntervalSeconds());

        if (result.isSuccess()) {
            handleSuccessfulCheck(endpoint, now, nextCheck);
        } else {
            handleFailedCheck(endpoint, result, now, nextCheck);
        }

        return result;
    }

    /**
     * Handle a successful check - reset failures, close incidents
     */
    private void handleSuccessfulCheck(Endpoint endpoint, LocalDateTime now, 
                                        LocalDateTime nextCheck) {
        boolean wasDown = endpoint.getStatus() == Endpoint.EndpointStatus.DOWN;

        // Update endpoint status
        endpointRepository.updateCheckStatus(
                endpoint.getId(),
                Endpoint.EndpointStatus.UP,
                now,
                nextCheck,
                0
        );

        // Close any open incidents
        if (wasDown) {
            int resolved = incidentRepository.resolveOpenIncident(endpoint.getId(), now);
            if (resolved > 0) {
                log.info("Resolved incident for endpoint {}", endpoint.getId());
                // Send recovery alert
                alertService.sendRecoveryAlert(endpoint);
            }
        }
    }

    /**
     * Handle a failed check - increment failures, create/update incidents, send alerts
     */
    private void handleFailedCheck(Endpoint endpoint, CheckResult result,
                                    LocalDateTime now, LocalDateTime nextCheck) {
        int newFailureCount = endpoint.getConsecutiveFailures() + 1;

        // Update endpoint status
        endpointRepository.updateCheckStatus(
                endpoint.getId(),
                Endpoint.EndpointStatus.DOWN,
                now,
                nextCheck,
                newFailureCount
        );

        // Find or create incident
        Incident incident = incidentRepository.findByEndpointIdAndStatus(
                endpoint.getId(), Incident.IncidentStatus.OPEN)
                .orElseGet(() -> {
                    Incident newIncident = Incident.create(
                            endpoint, result.getResultType(), result.getErrorMessage());
                    return incidentRepository.save(newIncident);
                });

        // Update incident with new failure
        incidentRepository.incrementFailureCount(incident.getId(), result.getErrorMessage());

        // Send alert if threshold reached
        alertService.sendFailureAlert(endpoint, result, incident);
    }

    /**
     * Generate a human-readable error message
     */
    private String generateErrorMessage(CheckResult.CheckResultType resultType,
                                        int actualStatus, int expectedStatus) {
        return switch (resultType) {
            case STATUS_MISMATCH -> String.format("Expected status %d but got %d", 
                                                  expectedStatus, actualStatus);
            case AUTH_FAILURE -> "Authentication failed (401 Unauthorized)";
            case SERVER_ERROR -> "Server error: HTTP " + actualStatus;
            case LATENCY_BREACH -> "Response time exceeded threshold";
            default -> "Check failed with status " + actualStatus;
        };
    }
}
