package com.abada.engine.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A Spring Web MVC interceptor that extracts user identity information from
 * request headers and stores it in the {@link IdentityContext}.
 * It ensures the context is cleared after the request is completed.
 */
@Component
public class IdentityContextInterceptor implements HandlerInterceptor {

    // OAuth2 Proxy headers (from oauth2-proxy ForwardAuth)
    private static final String OAUTH2_USER_HEADER = "X-Auth-Request-User";
    private static final String OAUTH2_GROUPS_HEADER = "X-Auth-Request-Groups";
    private static final String OAUTH2_EMAIL_HEADER = "X-Auth-Request-Email";

    // Legacy headers (for backward compatibility and direct header injection)
    private static final String USER_HEADER = "X-User";
    private static final String GROUPS_HEADER = "X-Groups";

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        // Priority: oauth2-proxy headers > legacy headers > anonymous
        String username = Optional.ofNullable(request.getHeader(OAUTH2_USER_HEADER))
                .or(() -> Optional.ofNullable(request.getHeader(USER_HEADER)))
                .or(() -> Optional.ofNullable(request.getHeader(OAUTH2_EMAIL_HEADER)))
                .orElse("anonymous");

        // Try oauth2-proxy groups header first, then fall back to legacy header
        List<String> groups = Optional.ofNullable(request.getHeader(OAUTH2_GROUPS_HEADER))
                .or(() -> Optional.ofNullable(request.getHeader(GROUPS_HEADER)))
                .map(header -> Arrays.stream(header.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList())
                .orElse(Collections.emptyList());

        IdentityContext.set(new Identity(username, groups));
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler, Exception ex) {
        IdentityContext.clear();
    }
}