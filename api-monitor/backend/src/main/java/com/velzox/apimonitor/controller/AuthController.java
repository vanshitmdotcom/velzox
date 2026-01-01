package com.velzox.apimonitor.controller;

import com.velzox.apimonitor.dto.ApiResponse;
import com.velzox.apimonitor.dto.AuthDto;
import com.velzox.apimonitor.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller - Handles user registration, login, and profile management
 * 
 * ENDPOINTS:
 * - POST /api/v1/auth/register - Register a new user
 * - POST /api/v1/auth/login - Authenticate and get JWT token
 * - GET /api/v1/auth/me - Get current user info
 * - PUT /api/v1/auth/profile - Update profile
 * - PUT /api/v1/auth/password - Change password
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user
     * 
     * Creates a new account with FREE plan and returns JWT token.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> register(
            @Valid @RequestBody AuthDto.RegisterRequest request) {
        
        log.info("Registration request for: {}", request.getEmail());
        
        AuthDto.AuthResponse response = authService.register(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Registration successful", response));
    }

    /**
     * Authenticate user and return JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.AuthResponse>> login(
            @Valid @RequestBody AuthDto.LoginRequest request) {
        
        log.info("Login request for: {}", request.getEmail());
        
        AuthDto.AuthResponse response = authService.login(request);
        
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    /**
     * Get current authenticated user info
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> getCurrentUser(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        AuthDto.UserInfo userInfo = authService.getCurrentUser(userDetails.getUsername());
        
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    /**
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<AuthDto.UserInfo>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AuthDto.UpdateProfileRequest request) {
        
        AuthDto.UserInfo userInfo = authService.updateProfile(
                userDetails.getUsername(), request);
        
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userInfo));
    }

    /**
     * Change password
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        
        authService.changePassword(userDetails.getUsername(), request);
        
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
