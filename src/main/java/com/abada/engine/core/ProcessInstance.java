package com.abada.engine.core;

import java.util.UUID;

/**
 * Represents a single runtime instance of a process.
 * Holds current position and instance-specific metadata.
 */
public class ProcessInstance {

    private final String id; // Unique ID of this instance
    private final ProcessDefinition definition; // Reference to the process definition
    private String currentElementId; // Current node in the flow (e.g. task ID)

    public ProcessInstance(ProcessDefinition definition) {
        this.id = UUID.randomUUID().toString();
        this.definition = definition;
        this.currentElementId = definition.getStartEventId(); // Start at the defined start event
    }

    public String getId() {
        return id;
    }

    public String getCurrentElementId() {
        return currentElementId;
    }

    public boolean isComplete() {
        return currentElementId == null;
    }

    public String advance() {
        // Get the next element in the flow from the current one
        String next = definition.getNextElement(currentElementId);
        currentElementId = next;
        return next;
    }

    public boolean isUserTask() {
        return definition.isUserTask(currentElementId);
    }

    public String getCurrentTaskName() {
        return definition.getTaskName(currentElementId);
    }
}

