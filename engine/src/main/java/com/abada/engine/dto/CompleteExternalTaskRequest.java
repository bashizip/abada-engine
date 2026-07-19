package com.abada.engine.dto;

import java.util.Map;

public record CompleteExternalTaskRequest(String workerId, Map<String, Object> variables) {
    public Map<String, Object> effectiveVariables() {
        return variables == null ? Map.of() : Map.copyOf(variables);
    }
}
