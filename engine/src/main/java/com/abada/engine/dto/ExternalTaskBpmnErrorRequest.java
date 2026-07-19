package com.abada.engine.dto;

import java.util.Map;

public record ExternalTaskBpmnErrorRequest(
        String workerId,
        String errorCode,
        String errorMessage,
        Map<String, Object> variables) {
    public Map<String, Object> effectiveVariables() {
        return variables == null ? Map.of() : Map.copyOf(variables);
    }
}
