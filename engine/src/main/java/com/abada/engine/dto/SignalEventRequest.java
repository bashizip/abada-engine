package com.abada.engine.dto;

import java.util.Map;

/**
 * Represents the payload for a request to broadcast a signal event.
 *
 * @param signalName The name of the signal to broadcast.
 * @param variables A map of variables to pass to the process instances.
 */
public record SignalEventRequest(String signalName, Map<String, Object> variables) {
}
