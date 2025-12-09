package com.abada.engine.dto;

import java.time.Instant;

public record ActiveJobDTO(
        String id,
        String type, // EXTERNAL_TASK, TIMER, MESSAGE, SIGNAL
        String processInstanceId,
        String activityId,
        Instant scheduledTime, // For timers/external tasks (lock exp)
        String details // Topic, Message Name, etc.
) {
}
