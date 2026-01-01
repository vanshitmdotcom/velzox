package com.velzox.apimonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * API Monitor Application - Main Entry Point
 * 
 * A developer-first API monitoring SaaS that:
 * - Periodically hits APIs (GET/POST)
 * - Supports authentication
 * - Measures availability (uptime), latency, and status correctness
 * - Sends real-time alerts when something breaks
 * 
 * @author Velzox Tech
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks for periodic monitoring
@EnableAsync       // Enable async processing for non-blocking HTTP calls
public class ApiMonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiMonitorApplication.class, args);
    }
}
