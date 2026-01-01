package com.velzox.apimonitor.security;

import com.velzox.apimonitor.entity.User;
import com.velzox.apimonitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * Custom UserDetailsService - Loads user data for Spring Security authentication
 * 
 * This service bridges our User entity with Spring Security's UserDetails interface.
 * It's used by:
 * - JWT authentication filter (to validate tokens)
 * - Login endpoint (to authenticate users)
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Load user by email (username in our case is email)
     * 
     * @param email User's email address
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if user not found
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email
                ));

        return buildUserDetails(user);
    }

    /**
     * Load user by ID (for internal operations)
     * 
     * @param id User's ID
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with id: " + id
                ));

        return buildUserDetails(user);
    }

    /**
     * Build Spring Security UserDetails from our User entity
     * 
     * @param user Our User entity
     * @return UserDetails for Spring Security
     */
    private UserDetails buildUserDetails(User user) {
        // Grant role based on user's plan
        String role = "ROLE_" + user.getPlan().name();
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isActive(),          // enabled
                true,                      // accountNonExpired
                true,                      // credentialsNonExpired
                true,                      // accountNonLocked
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }
}
