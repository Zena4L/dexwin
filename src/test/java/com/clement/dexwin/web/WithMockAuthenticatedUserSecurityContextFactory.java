package com.clement.dexwin.web;

import com.clement.dexwin.config.UserAuthentication;
import com.clement.dexwin.domain.models.users.Roles;
import com.clement.dexwin.domain.models.users.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Collections;
import java.util.UUID;

public class WithMockAuthenticatedUserSecurityContextFactory implements WithSecurityContextFactory<WithMockAuthenticatedUser> {

    @Override
    public SecurityContext createSecurityContext(WithMockAuthenticatedUser annotation) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();

        // Create a test user based on the annotation parameters
        User user = User.builder()
                .id(UUID.randomUUID())
                .firstName(annotation.firstName())
                .lastName(annotation.lastName())
                .email(annotation.email())
                .roles(Roles.valueOf(annotation.role()))
                .isActive(true)
                .isDeleted(false)
                .build();

        // Create UserAuthentication with the test user
        Authentication auth = new UserAuthentication(
                null, // JWT can be null in tests
                user,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + annotation.role()))
        );

        context.setAuthentication(auth);
        return context;
    }
}

