package com.abada.engine.core;

import com.abada.engine.core.model.GatewayMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.SequenceFlow;
import com.abada.engine.core.model.TaskMeta;
import com.abada.engine.dto.UserTaskPayload;
import com.abada.engine.util.ConditionEvaluator;

import java.util.*;

public class ProcessInstance {

    private final String id;
    private final ParsedProcessDefinition definition;
    private String currentActivityId;
    private final Map<String, Object> variables = new HashMap<>();

    public ProcessInstance(ParsedProcessDefinition definition) {
        this.id = UUID.randomUUID().toString();
        this.definition = definition;
        this.currentActivityId = definition.getStartEventId();
    }

    public ProcessInstance(String id, ParsedProcessDefinition definition, String currentActivityId) {
        this.id = id;
        this.definition = definition;
        this.currentActivityId = currentActivityId;
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

    public void setVariable(String key, Object value) {
        variables.put(key, value);
    }

    public Object getVariable(String key) {
        return variables.get(key);
    }

    public Map<String, Object> getVariables() {
        return Collections.unmodifiableMap(variables);
    }

    public boolean isWaitingForUserTask() {
        return currentActivityId != null && definition.isUserTask(currentActivityId);
    }

    public boolean isCompleted() {
        return currentActivityId == null;
    }

    public Optional<UserTaskPayload> advance() {
        if (currentActivityId == null) {
            return Optional.empty();
        }

        // Keep advancing until we hit a user task or the end of the process
        while (currentActivityId != null) {
            if (definition.isUserTask(currentActivityId)) {
                TaskMeta task = definition.getUserTask(currentActivityId);
                return Optional.of(new UserTaskPayload(
                        currentActivityId,
                        task.getName(),
                        task.getAssignee(),
                        task.getCandidateUsers(),
                        task.getCandidateGroups()
                ));
            }

            if (definition.isGateway(currentActivityId)) {
                GatewayMeta gateway = definition.getGateway(currentActivityId);
                List<SequenceFlow> outgoing = definition.getOutgoingFlows(currentActivityId);

                boolean conditionMet = false;
                for (SequenceFlow flow : outgoing) {
                    String expr = flow.getConditionExpression();
                    if (expr != null && !expr.isBlank()) {
                        if (ConditionEvaluator.evaluate(expr, variables)) {
                            currentActivityId = flow.getTargetRef();
                            conditionMet = true;
                            break; // Exit after finding the first valid flow
                        }
                    }
                }

                if (!conditionMet) {
                    // If no conditional flow was taken, try to find a default flow
                    for (SequenceFlow flow : outgoing) {
                        if (flow.isDefault()) {
                            currentActivityId = flow.getTargetRef();
                            conditionMet = true;
                            break;
                        }
                    }
                }

                if (!conditionMet) {
                    throw new IllegalStateException("No valid outgoing sequence flow from gateway: " + currentActivityId);
                }
            } else {
                // If it's not a gateway or user task, just move to the next activity
                currentActivityId = definition.getNextActivity(currentActivityId);
            }
        }

        return Optional.empty(); // End of process
    }
}
