package com.abada.engine.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1")
public class RootController {

    @GetMapping
    public Map<String, Object> index() {
        return Map.of("message", "Welcome to Abada Engine API");
    }
}
