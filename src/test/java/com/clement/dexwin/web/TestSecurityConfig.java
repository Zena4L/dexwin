package com.clement.dexwin.web;

import com.clement.dexwin.domain.models.users.User;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

import java.util.ArrayList;
import java.util.List;


@TestConfiguration
public class TestSecurityConfig {

    @Autowired
    private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

    @PostConstruct
    public void init() {
        List<HandlerMethodArgumentResolver> argumentResolvers =
                requestMappingHandlerAdapter.getArgumentResolvers() != null
                    ? new ArrayList<>(requestMappingHandlerAdapter.getArgumentResolvers())
                    : new ArrayList<>();

        // Add our custom resolver at the beginning to give it priority
        argumentResolvers.addFirst(new AuthenticationPrincipalArgumentResolver());
        requestMappingHandlerAdapter.setArgumentResolvers(argumentResolvers);
    }

    /**
     * Custom argument resolver that handles @AuthenticationPrincipal annotation in tests
     */
    private static class AuthenticationPrincipalArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && parameter.getParameterType().equals(User.class);
        }

        @Override
        @Nullable
        public Object resolveArgument(@NonNull MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
                                       @NonNull NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                return authentication.getPrincipal();
            }
            return null;
        }
    }
}

