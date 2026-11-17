package com.smarthealth.auth.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AuthenticatedUser implements UserDetails {

    private final User user;
    private final List<GrantedAuthority> authorities;

    public AuthenticatedUser(
            User user,
            List<SimpleGrantedAuthority> authorities) {

        this.user = user;
        this.authorities = authorities != null ? new ArrayList<>(authorities) : new ArrayList<>();
    }

    public User getUser() {
        return user;
    }

    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }

    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user != null ? user.getPasswordHash() : null;
    }

    @Override
    public String getUsername() {
        return user != null ? user.getEmail() : null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (user == null || user.getLockedUntil() == null) {
            return true;
        }

        return user.getLockedUntil().isBefore(OffsetDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user != null
                && "ACTIVE".equalsIgnoreCase(user.getStatus())
                && user.isEmailVerified();
    }
}
