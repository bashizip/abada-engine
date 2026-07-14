package com.abada.engine.core;

@FunctionalInterface
public interface LifecycleEventPublisher {
    void publish(PublishedLifecycleEvent event);
}
