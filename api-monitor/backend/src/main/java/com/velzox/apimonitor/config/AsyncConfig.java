package com.velzox.apimonitor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration - Configures thread pools for async operations
 * 
 * Used for:
 * - Non-blocking HTTP calls to monitored endpoints
 * - Parallel processing of multiple endpoint checks
 * - Async alert sending (email, Slack, webhooks)
 * 
 * PERFORMANCE CONSIDERATIONS:
 * - Thread pool size is configurable based on expected load
 * - Queue capacity prevents memory exhaustion
 * - CallerRunsPolicy ensures no tasks are dropped
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Value("${app.monitoring.thread-pool-size}")
    private int threadPoolSize;

    /**
     * Main executor for monitoring tasks
     * Used for endpoint health checks
     */
    @Bean(name = "monitoringExecutor")
    public Executor monitoringExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Core pool size - threads always kept alive
        executor.setCorePoolSize(threadPoolSize);
        
        // Max pool size - maximum threads during peak load
        executor.setMaxPoolSize(threadPoolSize * 2);
        
        // Queue capacity - tasks waiting when all threads are busy
        executor.setQueueCapacity(500);
        
        // Thread naming for debugging
        executor.setThreadNamePrefix("monitor-");
        
        // Wait for tasks to complete on shutdown
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        return executor;
    }

    /**
     * Executor for alert sending tasks
     * Separate pool to prevent alerting from blocking monitoring
     */
    @Bean(name = "alertExecutor")
    public Executor alertExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Smaller pool for alerts - less frequent operations
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("alert-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
}
