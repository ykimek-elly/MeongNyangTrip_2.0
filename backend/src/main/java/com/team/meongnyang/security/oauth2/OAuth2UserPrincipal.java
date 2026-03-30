package com.team.meongnyang.security.oauth2;

import com.team.meongnyang.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class OAuth2UserPrincipal implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;
    private final boolean newUser;

    public OAuth2UserPrincipal(User user, Map<String, Object> attributes, boolean newUser) {
        this.user = user;
        this.attributes = attributes;
        this.newUser = newUser;
    }

    public User getUser() {
        return user;
    }

    public boolean isNewUser() {
        return newUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getName() {
        return user.getEmail();
    }
}
