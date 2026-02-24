Abada Platform: Full Multi-Tenant CORS & Vary ConfigurationThis document provides the complete technical setup for handling dynamic customer subdomains (*.localhost and *.abada.dev) using Spring Boot 3.5.x and Traefik.1. Spring Boot Engine ConfigurationThis configuration handles the CORS preflight logic and ensures the Vary: Origin header is correctly set to prevent cache poisoning between tenants.SecurityConfig.javapackage io.abada.engine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Collections;

@Configuration
public class SecurityConfig {

    @Value("${abada.domain:abada.dev}")
    private String platformDomain;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable()) 
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
            
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        return new CorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                String origin = request.getHeader("Origin");
                CorsConfiguration config = new CorsConfiguration();

                // DYNAMIC ORIGIN VALIDATION
                if (isValidOrigin(origin)) {
                    // We echo the specific origin. This automatically triggers 
                    // Spring to add "Vary: Origin" to the response.
                    config.setAllowedOrigins(List.of(origin));
                } else {
                    // If invalid, we return a configuration that doesn't allow the origin
                    return null; 
                }

                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
                config.setAllowCredentials(true);
                config.setMaxAge(3600L);
                
                return config;
            }

            private boolean isValidOrigin(String origin) {
                if (origin == null) return false;

                // 1. Dev Environment
                if (origin.endsWith(".localhost") || origin.startsWith("http://localhost:")) return true;

                // 2. Production Subdomains
                if (origin.endsWith("." + platformDomain)) return true;

                // 3. TODO: Customer Custom Domain Database Lookup
                return false;
            }
        };
    }
}
2. The "Vary" Header InstructionThe Vary: Origin header tells Traefik and browser caches: "The content of this response is different depending on who is asking (the Origin)."Why it's crucial for Abada:Without this, if customer1.abada.dev makes a request and Traefik caches the response (with Access-Control-Allow-Origin: customer1.abada.dev), then customer2.abada.dev might receive that cached header and be blocked because the origin doesn't match.How we ensure it:Spring Auto-Behavior: By using config.setAllowedOrigins(List.of(origin)) with a specific value instead of *, Spring Security’s DefaultCorsProcessor automatically adds Vary: Origin.Verification: In your browser DevTools, ensure that every REST API response contains:Vary: Origin, Access-Control-Request-Method, Access-Control-Request-Headers3. Traefik Docker-Compose (Production & Dev)Remove all Traefik-level CORS middlewares. Use this label structure to route all subdomains to your Engine.services:
  engine:
    image: abada-engine:latest
    labels:
      - "traefik.enable=true"
      # Regex matches any subdomain for your dev and prod domains
      - "traefik.http.routers.engine.rule=HostRegexp(`{subdomain:[a-z0-9-]+}.localhost`) || HostRegexp(`{subdomain:[a-z0-9-]+}.abada.dev`)"
      - "traefik.http.routers.engine.entrypoints=web"
      # CRITICAL: Do NOT add traefik.http.middlewares.headers... here.
      # Spring Boot handles the headers.
4. Keycloak Global ConfigurationTo support the dynamic nature of your SaaS, configure your Keycloak client:Root URL: Leave empty.Valid Redirect URIs: - http://*.localhost:*/*https://*.abada.dev/*Web Origins: + (Must be exactly the plus sign).
