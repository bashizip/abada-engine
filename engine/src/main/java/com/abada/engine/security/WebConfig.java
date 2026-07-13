package com.abada.engine.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final IdentityContextInterceptor identityContextInterceptor;
    private final String[] allowedOrigins;

    public WebConfig(IdentityContextInterceptor identityContextInterceptor,
            @Value("${abada.security.allowed-origins:http://localhost:5173,http://localhost:5602}") String allowedOrigins) {
        this.identityContextInterceptor = identityContextInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toArray(String[]::new);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(identityContextInterceptor);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    /**
     * Creates a filter bean to log incoming request details for debugging purposes.
     * This provides a clear, detailed view of every request, including headers and
     * payload.
     *
     * @return The configured logging filter.
     */
    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(false);
        filter.setIncludeHeaders(false);
        filter.setAfterMessagePrefix("REQUEST DATA : ");
        return filter;
    }
}
