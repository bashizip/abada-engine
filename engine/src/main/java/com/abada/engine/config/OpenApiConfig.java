package com.abada.engine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

        @Value("${spring.application.version:v0.8.4-alpha}")
        private String appVersion;

        @Value("${spring.application.name:Abada Engine}")
        private String appName;

        @Bean
        public OpenAPI abadaEngineOpenAPI() {
                return new OpenAPI()
                                .info(new Info()
                                                .title(appName + " API")
                                                .description("High-performance, modular BPMN 2.0 process automation engine.")
                                                .version(appVersion)
                                                .contact(new Contact()
                                                                .name("Abada Platform")
                                                                .url("https://github.com/bashizip/abada-engine"))
                                                .license(new License()
                                                                .name("MIT")
                                                                .url("https://opensource.org/licenses/MIT")));
        }
}
