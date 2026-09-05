package com.healthcare.patient.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security principal for an authenticated caller. Built from the
 * claims of a verified JWT. The Auth Service database is not consulted
 * on every request.
 */
public class PatientPrincipal implements UserDetails {

    private final UUID userId;
    private final Role role;

    public PatientPrincipal(UUID userId, Role role) {
        this.userId = userId;
        this.role = role;
    }

    public UUID getUserId() { return userId; }
    public Role getRole() { return role; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override public String getPassword() { return ""; }
    @Override public String getUsername() { return userId.toString(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
