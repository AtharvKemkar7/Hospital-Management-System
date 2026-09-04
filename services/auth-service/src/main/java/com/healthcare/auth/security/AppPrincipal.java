package com.healthcare.auth.security;

import com.healthcare.auth.entity.AccountStatus;
import com.healthcare.auth.entity.Role;
import com.healthcare.auth.entity.User;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security principal for an authenticated Auth Service user.
 *
 * <p>Holds the user identity, the role, and the account status. The
 * status is exposed so security filters and services can enforce
 * "must be ACTIVE" rules.
 */
public class AppPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Role role;
    private final AccountStatus status;

    public AppPrincipal(UUID id, String email, String passwordHash,
                        Role role, AccountStatus status) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = status;
    }

    public static AppPrincipal from(User user) {
        return new AppPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.getStatus()
        );
    }

    public UUID getId() { return id; }
    public Role getRole() { return role; }
    public AccountStatus getStatus() { return status; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.authority()));
    }

    @Override public String getPassword() { return passwordHash; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() {
        return status == AccountStatus.ACTIVE;
    }

    @Override public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() {
        return status == AccountStatus.ACTIVE;
    }
}
