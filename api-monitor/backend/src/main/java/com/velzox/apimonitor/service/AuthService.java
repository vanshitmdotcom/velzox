package com.velzox.apimonitor.service;

import com.velzox.apimonitor.dto.AuthDto;
import com.velzox.apimonitor.entity.User;
import com.velzox.apimonitor.exception.DuplicateResourceException;
import com.velzox.apimonitor.repository.UserRepository;
import com.velzox.apimonitor.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication Service - Handles user registration and login
 * 
 * FEATURES:
 * - User registration with email uniqueness validation
 * - Secure password hashing with BCrypt
 * - JWT token generation for authenticated sessions
 * - Login with email/password validation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Register a new user
     * 
     * @param request Registration details (name, email, password)
     * @return Auth response with JWT token and user info
     * @throws DuplicateResourceException if email already exists
     */
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed - email already exists: {}", request.getEmail());
            throw DuplicateResourceException.email(request.getEmail());
        }

        // Create new user with hashed password
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .plan(User.Plan.FREE)  // New users start on FREE plan
                .emailVerified(false)
                .active(true)
                .build();

        // Save user to database
        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getId());

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(user.getEmail());

        return AuthDto.AuthResponse.of(user, token, jwtTokenProvider.getExpirationMs());
    }

    /**
     * Authenticate user and generate JWT token
     * 
     * @param request Login credentials (email, password)
     * @return Auth response with JWT token and user info
     */
    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        log.info("Login attempt for: {}", request.getEmail());

        // Authenticate using Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Get user from database
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(authentication);

        log.info("User logged in successfully: {}", user.getId());

        return AuthDto.AuthResponse.of(user, token, jwtTokenProvider.getExpirationMs());
    }

    /**
     * Get current user information
     * 
     * @param email User's email from JWT token
     * @return User info DTO
     */
    @Transactional(readOnly = true)
    public AuthDto.UserInfo getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return AuthDto.UserInfo.from(user);
    }

    /**
     * Change user password
     * 
     * @param email User's email
     * @param request Current and new password
     */
    @Transactional
    public void changePassword(String email, AuthDto.ChangePasswordRequest request) {
        log.info("Password change request for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", user.getId());
    }

    /**
     * Update user profile
     * 
     * @param email User's email
     * @param request Profile update data
     * @return Updated user info
     */
    @Transactional
    public AuthDto.UserInfo updateProfile(String email, AuthDto.UpdateProfileRequest request) {
        log.info("Profile update request for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setName(request.getName());
        user = userRepository.save(user);

        log.info("Profile updated for user: {}", user.getId());

        return AuthDto.UserInfo.from(user);
    }
}
