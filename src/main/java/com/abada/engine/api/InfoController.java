package com.abada.engine.api;

import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/abada")
public class InfoController {

    private final BuildProperties buildProperties;

    public InfoController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        return Map.of(
                "status", "UP",
                "engineVersion", buildProperties.getVersion(),
                "bpmnSupport", "BPMN model Validation"
        );
    }
}
