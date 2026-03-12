package org.app.mintonmatchapi.auth;

import lombok.Getter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements Authentication {

    private final Long userId;
    private final Role role;

    public UserPrincipal(Long userId) {
        this(userId, Role.USER);
    }

    public UserPrincipal(Long userId, Role role) {
        this.userId = userId;
        this.role = role != null ? role : Role.USER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return userId;
    }

    @Override
    public boolean isAuthenticated() {
        return true;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) {
        // immutable
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
