package com.velzox.apimonitor.service;

import com.velzox.apimonitor.entity.User;
import com.velzox.apimonitor.exception.ResourceNotFoundException;
import com.velzox.apimonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Service - Utility service for user operations
 * 
 * Provides helper methods for:
 * - Looking up user IDs by email
 * - Checking user permissions
 * - Plan limit validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get user ID by email
     * 
     * @param email User's email address
     * @return User's ID
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
        return user.getId();
    }

    /**
     * Get user by email
     * 
     * @param email User's email address
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    /**
     * Get user by ID
     * 
     * @param userId User's ID
     * @return User entity
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    /**
     * Check if user can add more endpoints
     * 
     * @param userId User's ID
     * @return true if under limit, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean canAddEndpoint(Long userId) {
        User user = getUserById(userId);
        long currentEndpoints = userRepository.countEndpointsByUserId(userId);
        return currentEndpoints < user.getPlan().getMaxEndpoints();
    }

    /**
     * Get remaining endpoint capacity
     * 
     * @param userId User's ID
     * @return Number of endpoints user can still add
     */
    @Transactional(readOnly = true)
    public int getRemainingEndpointCapacity(Long userId) {
        User user = getUserById(userId);
        long currentEndpoints = userRepository.countEndpointsByUserId(userId);
        return Math.max(0, user.getPlan().getMaxEndpoints() - (int) currentEndpoints);
    }

    /**
     * Check if check interval is allowed for user's plan
     * 
     * @param userId User's ID
     * @param intervalSeconds Requested check interval
     * @return true if allowed, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isCheckIntervalAllowed(Long userId, int intervalSeconds) {
        User user = getUserById(userId);
        return intervalSeconds >= user.getPlan().getMinCheckIntervalSeconds();
    }

    /**
     * Check if Slack alerts are enabled for user's plan
     * 
     * @param userId User's ID
     * @return true if Slack is enabled
     */
    @Transactional(readOnly = true)
    public boolean isSlackEnabled(Long userId) {
        User user = getUserById(userId);
        return user.getPlan().isSlackEnabled();
    }
}
