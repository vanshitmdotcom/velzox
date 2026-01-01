package com.velzox.apimonitor.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient Configuration - Configures async HTTP client for endpoint monitoring
 * 
 * Uses Spring WebFlux's WebClient for non-blocking HTTP calls.
 * 
 * FEATURES:
 * - Connection timeout configuration
 * - Read/Write timeout configuration
 * - Connection pooling (managed by Reactor Netty)
 * - SSL/TLS support for HTTPS endpoints
 * 
 * PERFORMANCE:
 * - Non-blocking I/O allows high concurrency
 * - Single WebClient instance is shared (thread-safe)
 * - Memory efficient compared to RestTemplate
 */
@Configuration
public class WebClientConfig {

    @Value("${app.monitoring.default-timeout-ms}")
    private int defaultTimeoutMs;

    /**
     * Create WebClient bean for monitoring requests
     * 
     * Note: Individual requests can override the timeout
     * when checking endpoints with custom timeout settings
     */
    @Bean
    public WebClient webClient() {
        // Configure Netty HTTP client with timeouts
        HttpClient httpClient = HttpClient.create()
                // Connection timeout - time to establish connection
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, defaultTimeoutMs)
                
                // Response timeout - total time for response
                .responseTimeout(Duration.ofMillis(defaultTimeoutMs))
                
                // Handler for read/write timeouts
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(
                                defaultTimeoutMs, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(
                                defaultTimeoutMs, TimeUnit.MILLISECONDS))
                );

        // Build WebClient with custom HTTP client
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // Set a large buffer size for responses (we don't store them anyway)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB max
                .build();
    }
}
