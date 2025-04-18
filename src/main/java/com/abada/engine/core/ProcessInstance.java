package com.abada.engine.core;

import java.util.UUID;

/**
 * Represents a running instance of a BPMN process.
 */
public class ProcessInstance {

    private final String id = UUID.randomUUID().toString();
    private final ProcessDefinition definition;
    private String currentElementId;

    public ProcessInstance(ProcessDefinition definition) {
        this.definition = definition;
        this.currentElementId = definition.getStartEventId();
    }

    public String getId() {
        return id;
    }

    public ProcessDefinition getDefinition() {
        return definition;
    }

    public String getCurrentElementId() {
        return currentElementId;
    }

    /**
     * Moves the process forward to the next element and returns the new element ID.
     * Returns null if no next element exists.
     */
    public String advance() {
        String next = definition.getNextElement(currentElementId);
        currentElementId = next;
        return next;
    }

    public boolean isUserTask() {
        return definition.isUserTask(currentElementId);
    }
}
