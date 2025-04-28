package com.abada.engine.core;

import java.util.UUID;

/**
 * Represents a running instance of a BPMN process.
 */
public class ProcessInstance {

    private final String id = UUID.randomUUID().toString();
    private final ParsedProcessDefinition definition;
    private String currentActivityId;

    public ProcessInstance(ParsedProcessDefinition definition) {
        this.definition = definition;
        this.currentActivityId = definition.getStartEventId();
    }

    public String getId() {
        return id;
    }

    public ParsedProcessDefinition getDefinition() {
        return definition;
    }

    public String getCurrentActivityId() {
        return currentActivityId;
    }

    /**
     * Moves the process forward to the next element and returns the new element ID.
     * Returns null if no next element exists.
     */
    public String advance() {
        String next = definition.getNextElement(currentActivityId);
        currentActivityId = next;
        return next;
    }

    public boolean isWaitingForUserTask() {
        // Implement logic to check if current activity is a user task
        return getDefinition().isUserTask(getCurrentActivityId());
    }

    public boolean isCompleted() {
        return getCurrentActivityId() == null;
    }


    public boolean isUserTask() {
        return definition.isUserTask(currentActivityId);
    }
}
