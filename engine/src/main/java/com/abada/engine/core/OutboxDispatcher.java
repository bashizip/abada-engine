package com.abada.engine.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "abada.outbox.dispatcher.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxDispatcher {
    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private final OutboxService outbox;
    private final LifecycleEventPublisher publisher;
    private final String owner = UUID.randomUUID().toString();

    public OutboxDispatcher(OutboxService outbox, LifecycleEventPublisher publisher) {
        this.outbox = outbox;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${abada.outbox.poll-interval-ms:1000}")
    public void dispatch() {
        for (PublishedLifecycleEvent event : outbox.claim(owner, 50, Instant.now())) {
            try {
                publisher.publish(event);
                outbox.markPublished(event.id(), owner, Instant.now());
            } catch (Exception exception) {
                log.warn("Lifecycle event delivery failed for {}: {}", event.id(), exception.getMessage());
                outbox.markFailed(event.id(), owner, exception.getMessage(), Instant.now());
            }
        }
    }
}
