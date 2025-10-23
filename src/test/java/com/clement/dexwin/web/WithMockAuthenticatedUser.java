package com.clement.dexwin.web;

import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockAuthenticatedUserSecurityContextFactory.class)
public @interface WithMockAuthenticatedUser {
    String email() default "john.doe@example.com";
    String firstName() default "John";
    String lastName() default "Doe";
    String role() default "VIEWER";
}

