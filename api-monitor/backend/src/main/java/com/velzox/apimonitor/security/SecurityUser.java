package com.velzox.apimonitor.security;

import com.velzox.apimonitor.entity.User;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Security User - Custom UserDetails implementation that wraps our User entity
 * 
 * This class provides a bridge between our domain User entity and 
 * Spring Security's UserDetails interface, allowing us to:
 * - Access the full User entity in controllers via @AuthenticationPrincipal
 * - Maintain proper separation of concerns
 */
@Getter
public class SecurityUser implements UserDetails {

    private final User user;
    private final Collection<SimpleGrantedAuthority> authorities;

    public SecurityUser(User user) {
        this.user = user;
        this.authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getPlan().name())
        );
    }

    /**
     * Get the user ID for quick access
     */
    public Long getId() {
        return user.getId();
    }

    /**
     * Get user's subscription plan
     */
    public User.Plan getPlan() {
        return user.getPlan();
    }

    @Override
    public Collection<SimpleGrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
