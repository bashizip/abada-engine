package com.abada.engine.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.abada.engine.persistence.repository")
public class PersistenceConfig {
    // Spring Boot auto-configures H2, so no manual initialization needed for now.
}
