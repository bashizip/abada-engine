package com.abada.engine.security;

import com.abada.engine.api.ApiErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class ProxyHeaderAuthenticationFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;

    public ProxyHeaderAuthenticationFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/v1".equals(path) || "/v1/info".equals(path) || "/actuator/health".equals(path)
                || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String username = firstNonBlank(request.getHeader("X-Auth-Request-User"),
                request.getHeader("X-Auth-Request-Email"));
        if (username == null) {
            ApiSecurityHandlers.write(objectMapper, request, response, HttpStatus.UNAUTHORIZED,
                    ApiErrorCode.AUTHENTICATION_REQUIRED, "Trusted proxy identity headers are missing");
            return;
        }

        List<String> groups = split(request.getHeader("X-Auth-Request-Groups"));
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        groups.stream().map(AbadaRoles::fromGroup).filter(java.util.Objects::nonNull)
                .map(SimpleGrantedAuthority::new).forEach(authorities::add);
        split(request.getHeader("X-Auth-Request-Permissions")).stream()
                .map(value -> value.startsWith("SCOPE_") ? value : "SCOPE_" + value)
                .map(SimpleGrantedAuthority::new).forEach(authorities::add);

        var authentication = UsernamePasswordAuthenticationToken.authenticated(username, "N/A", authorities);
        authentication.setDetails(groups);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(",")).map(String::strip).filter(item -> !item.isEmpty()).toList();
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) return first;
        if (second != null && !second.isBlank()) return second;
        return null;
    }
}
