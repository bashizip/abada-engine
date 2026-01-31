package com.abada.engine.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/info")
public class InfoController {

    @Value("${spring.application.version}")
    private String appVersion;

    @Value("${spring.application.name}")
    private String appName;

    @Value("${spring.profiles.active}")
    private String profile;

    public InfoController() {

    }

    @GetMapping
    public Map<String, Object> info() {
        return Map.of(
                "name", appName,
                "description", "Abada Engine - High Performance BPMN Execution",
                "version", appVersion,
                "status", "UP",
                "profile", profile,
                "runtime", Map.of(
                        "javaVersion", System.getProperty("java.version"),
                        "timestamp", java.time.Instant.now().toString(),
                        "uptime", java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime() + "ms"),
                "capabilities", Map.of(
                        "bpmn", "Core engine features implemented",
                        "cmmn", "Planned",
                        "dmn", "Planned"));
    }
}
