package com.abada.engine.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class ProxyHeaderAuthenticationFilterTest {
    private final ProxyHeaderAuthenticationFilter filter = new ProxyHeaderAuthenticationFilter(
            new ObjectMapper().findAndRegisterModules());

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsRequestsWithoutHeadersFromTheConfiguredTrustedProxyMode() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest(), response, noOpChain());
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("AUTHENTICATION_REQUIRED");
    }

    @Test
    void mapsOnlyExplicitAbadaSecurityGroupsToAuthorities() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Auth-Request-User", "alice");
        request.addHeader("X-Auth-Request-Groups", "customers,abada-task-user");
        filter.doFilter(request, new MockHttpServletResponse(), noOpChain());

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.getName()).isEqualTo("alice");
        assertThat(authentication.getAuthorities()).extracting(Object::toString)
                .containsExactly(AbadaRoles.TASK_USER);
    }

    private FilterChain noOpChain() {
        return (request, response) -> { };
    }
}
