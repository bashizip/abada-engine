package com.abada.engine.dto;

import java.util.Map;

/**
 * Request body for patching process variables.
 */
public record VariablePatchRequest(
        Map<String, VariableValue> modifications) {
}
