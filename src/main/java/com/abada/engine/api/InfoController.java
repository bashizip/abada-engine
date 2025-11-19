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
                "status", "UP",
                "engineVersion", appVersion,
                   "profile", profile,
                "bpmnSupport", "Core engine features implemented."
        );
    }
}
