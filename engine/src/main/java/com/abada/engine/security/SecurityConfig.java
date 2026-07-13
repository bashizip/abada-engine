package com.abada.engine.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Value("${abada.security.mode:disabled}") String mode) throws Exception {
        http.csrf(csrf -> csrf.disable());

        if ("oidc".equalsIgnoreCase(mode)) {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/v1/info", "/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/v1/processes/deploy").hasAuthority("SCOPE_process:deploy")
                    .requestMatchers("/v1/jobs/**", "/v1/process-instances/**").hasAuthority("SCOPE_operations:write")
                    .requestMatchers("/v1/external-tasks/**").hasAuthority("SCOPE_worker:execute")
                    .anyRequest().authenticated())
                    .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        } else {
            // "proxy" is valid only when the engine is unreachable except through the
            // authenticated reverse proxy. "disabled" is intended for local/test use.
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        }
        return http.build();
    }
}
