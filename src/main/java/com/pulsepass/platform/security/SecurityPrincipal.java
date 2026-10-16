package com.pulsepass.platform.security;

import com.pulsepass.platform.domain.Role;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;



public final class SecurityPrincipal implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final List<SimpleGrantedAuthority> authorities;

    public SecurityPrincipal(Long userId, String username, String email,
                             String passwordHash, boolean active, Collection<Role> roles) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.authorities = roles.stream()
                .sorted()
                .map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }

    public boolean hasRole(Role role) {
        return authorities.stream().anyMatch(a -> a.getAuthority().equals(role.authority()));
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public String getPassword() { return passwordHash; }

    @Override
    public String getUsername() { return username; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    /** Un usuario desactivado (active = false) no está habilitado. */
    @Override
    public boolean isEnabled() { return active; }

    /** Nunca incluye el hash. */
    @Override
    public String toString() {
        return "SecurityPrincipal[userId=" + userId + ", username=" + username + ", authorities=" + authorities + "]";
    }
}
