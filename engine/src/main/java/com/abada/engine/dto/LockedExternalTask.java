package com.abada.engine.dto;

import java.util.Map;
import java.time.Instant;

/**
 * Represents an external task that has been locked for a worker.
 * This is the payload returned by the fetch-and-lock API.
 *
 * @param id The unique ID of the external task.
 * @param topicName The topic of the task.
 * @param variables The process variables available to the task.
 */
public record LockedExternalTask(
        String id,
        String topicName,
        Map<String, Object> variables,
        String processInstanceId,
        String activityId,
        Integer retries,
        Instant lockExpirationTime,
        String traceParent,
        String protocolVersion) {

    public LockedExternalTask(String id, String topicName, Map<String, Object> variables) {
        this(id, topicName, variables, null, null, null, null, null, "1");
    }
}
