package com.abada.engine.core;

import java.time.Instant;

public record PublishedLifecycleEvent(
        String id,
        String aggregateType,
        String aggregateId,
        String eventType,
        String payloadJson,
        Instant occurredAt) {
}
