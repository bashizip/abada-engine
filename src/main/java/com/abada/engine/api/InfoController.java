package com.abada.engine.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/info")
public class InfoController {

    @Value("${app.version}")
    private String appVersion;

    public InfoController() {

    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "status", "UP",
                "engineVersion", appVersion,
                "bpmnSupport", "BPMN model Validation"
        );
    }
}
