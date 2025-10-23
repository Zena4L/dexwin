package com.clement.dexwin.config;

import com.clement.dexwin.domain.models.users.User;
import lombok.EqualsAndHashCode;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

@EqualsAndHashCode(callSuper = true)
public class UserAuthentication extends AbstractAuthenticationToken {
    /**
     * The original JWT token used for authentication.
     */
    private final Jwt jwt;

    /**
     * The User entity representing the authenticated principal.
     */
    private final transient User principal;

    /**
     * Constructs a new UserAuthentication token.
     *
     * @param jwt         The JWT token used for authentication
     * @param principal   The User entity representing the authenticated principal
     * @param authorities The granted authorities for this authentication
     */
    public UserAuthentication(Jwt jwt, User principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        this.principal = principal;
        setAuthenticated(true);
    }

    /**
     * @return The JWT token as credentials
     */
    @Override
    public Object getCredentials() {
        return jwt;
    }

    /**
     * @return The username of the User entity as authentication name
     */
    @Override
    public String getName() {
        return principal.getEmail();
    }

    /**
     * @return The User entity as principal
     */
    @Override
    public Object getPrincipal() {
        return principal;
    }
}