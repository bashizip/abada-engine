package com.abada.engine.core;

import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.dto.UserTaskPayload;

import java.util.Optional;
import java.util.UUID;

public class ProcessInstance {

    private final String id;
    private final ParsedProcessDefinition definition;
    private String currentActivityId;

    // Standard constructor used when starting a new process
    public ProcessInstance(ParsedProcessDefinition definition) {
        this.id = UUID.randomUUID().toString();
        this.definition = definition;
        this.currentActivityId = definition.getStartEventId();
    }

    public ProcessInstance(String id, ParsedProcessDefinition definition, String currentActivityId) {
        this.id = id;
        // IMPORTANT: during reload, we need to reparse the BPMN definition
        this.definition = definition;
        this.currentActivityId = currentActivityId;
        // (Status is managed by currentActivityId already, no need to store it here explicitly)
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

    public void setCurrentActivityId(String currentActivityId) {
        this.currentActivityId = currentActivityId;
    }

    public boolean isWaitingForUserTask() {
        return currentActivityId != null && definition.isUserTask(currentActivityId);
    }

    public boolean isCompleted() {
        return currentActivityId == null;
    }

    public Optional<UserTaskPayload> advance() {
        String next = definition.getNextActivity(currentActivityId);
        currentActivityId = next;

        if (next == null) {
            return Optional.empty(); // End of process
        }

        if (definition.isUserTask(next)) {
            return Optional.of(new UserTaskPayload(
                    next,
                    definition.getTaskName(next),
                    definition.getTaskAssignee(next),
                    definition.getCandidateUsers(next),
                    definition.getCandidateGroups(next)
            ));
        }

        // In the future: skip service tasks, gateways, etc.
        // For now, assume only userTasks and endEvents exist
        return advance(); // recursively skip non-user tasks if needed
    }

}
