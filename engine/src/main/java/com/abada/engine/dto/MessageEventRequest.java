package com.abada.engine.dto;

import java.util.Map;

/**
 * Represents the payload for a request to correlate a message event.
 *
 * @param messageName The name of the message to correlate.
 * @param correlationKey The business key to identify the specific process instance.
 * @param variables A map of variables to pass to the process instance.
 */
public record MessageEventRequest(String messageName, String correlationKey, Map<String, Object> variables) {
}
