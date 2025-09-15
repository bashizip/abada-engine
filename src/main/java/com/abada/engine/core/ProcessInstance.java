package com.abada.engine.core;

import com.abada.engine.core.model.GatewayMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.SequenceFlow;
import com.abada.engine.core.model.TaskMeta;
import com.abada.engine.dto.UserTaskPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ProcessInstance {

    private String id;
    private ParsedProcessDefinition definition;
    private final Map<String, Object> variables = new HashMap<>();

    // List of active execution pointers (activity IDs)
    private final List<String> activeTokens = new ArrayList<>();

    // Tracks how many tokens are expected at a joining gateway
    // Key: Gateway ID, Value: Number of expected tokens
    private final Map<String, Integer> joinExpectedTokens = new HashMap<>();

    // Tracks which tokens have arrived at a joining gateway
    // Key: Gateway ID, Value: Set of incoming source activity IDs
    private final Map<String, Set<String>> joinArrivedTokens = new HashMap<>();


    private static final Logger log = LoggerFactory.getLogger(ProcessInstance.class);

    public ProcessInstance(ParsedProcessDefinition definition) {
        this.id = UUID.randomUUID().toString();
        this.definition = definition;
        // Start with a single token at the start event
        this.activeTokens.add(definition.getStartEventId());
    }

    public ProcessInstance(String id, ParsedProcessDefinition definition, List<String> activeTokens) {
        this.id = id;
        this.definition = definition;
        this.activeTokens.addAll(activeTokens);
    }

    public ProcessInstance() {

    }

    public String getId() { return id; }
    public ParsedProcessDefinition getDefinition() { return definition; }
    public List<String> getActiveTokens() { return Collections.unmodifiableList(activeTokens); }
    public void setActiveTokens(List<String> tokens) {
        activeTokens.clear();
        activeTokens.addAll(tokens);
    }

    public void setVariable(String key, Object value) { variables.put(key, value); }
    public Object getVariable(String key) { return variables.get(key); }
    public Map<String, Object> getVariables() { return Collections.unmodifiableMap(variables); }
    public void putVariable(String key, Object value) { variables.put(key, value); }
    public void putAllVariables(Map<String,Object> newVars) {
        if (newVars != null) variables.putAll(newVars);
    }

    public boolean isWaitingForUserTask() {
        return !activeTokens.isEmpty() && activeTokens.stream()
                .anyMatch(t -> definition.isUserTask(t));
    }

    public boolean isCompleted() { return activeTokens.isEmpty(); }

    public List<UserTaskPayload> advance() {
        return advance(null);
    }


    public List<UserTaskPayload> advance(String completedUserTask) {
        List<UserTaskPayload> newUserTasks = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();

        if (completedUserTask != null) {
            // When a task is completed, only its path is advanced.
            // Other active tokens are parallel paths that are still waiting.
            activeTokens.remove(completedUserTask);
            queue.add(completedUserTask);
        } else {
            // This case is for the initial process start.
            queue.addAll(activeTokens);
            activeTokens.clear();
        }

        Set<String> processedInThisRun = new HashSet<>();

        while (!queue.isEmpty()) {
            String current = queue.poll();
            String previousPointer = null; // Track the node before the current one

            if (processedInThisRun.contains(current)) {
                continue;
            }

            int hops = 0;
            final int MAX_HOPS = 2048;

            while (current != null && !processedInThisRun.contains(current)) {
                if (++hops > MAX_HOPS) {
                    throw new IllegalStateException("advance() exceeded max hops; possible cycle without wait state. pi=" + id);
                }

                String pointer = current;
                processedInThisRun.add(pointer);

                // USER TASK
                if (definition.isUserTask(pointer)) {
                    if (Objects.equals(pointer, completedUserTask)) {
                        var outgoing = definition.getOutgoing(pointer);
                        current = outgoing.isEmpty() ? null : outgoing.get(0).getTargetRef();
                        previousPointer = pointer;
                    } else {
                        activeTokens.add(pointer);
                        TaskMeta ut = definition.getUserTask(pointer);
                        newUserTasks.add(new UserTaskPayload(ut.getId(), ut.getName(), ut.getAssignee(), ut.getCandidateUsers(), ut.getCandidateGroups()));
                        current = null;
                    }
                }
                // EXCLUSIVE GATEWAY
                else if (definition.isExclusiveGateway(pointer)) {
                    GatewaySelector selector = new GatewaySelector();
                    GatewayMeta gw = definition.getGateways().get(pointer);
                    List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
                    String chosenFlowId = selector.chooseOutgoing(gw, outgoing, variables);
                    previousPointer = pointer;
                    current = outgoing.stream()
                            .filter(f -> Objects.equals(f.getId(), chosenFlowId))
                            .map(SequenceFlow::getTargetRef)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Flow not found: " + chosenFlowId));
                }
                // PARALLEL GATEWAY (FORK)
                else if (definition.isParallelGateway(pointer) && definition.getIncoming(pointer).size() == 1) {
                    List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
                    for (SequenceFlow flow : outgoing) {
                        queue.add(flow.getTargetRef());
                    }
                    String joinGatewayId = definition.findJoinGateway(pointer, GatewayMeta.Type.PARALLEL);
                    if (joinGatewayId != null) {
                        joinExpectedTokens.put(joinGatewayId, definition.getIncoming(joinGatewayId).size());
                        joinArrivedTokens.put(joinGatewayId, new HashSet<>());
                    }
                    current = null;
                }
                // INCLUSIVE GATEWAY (FORK)
                else if (definition.isInclusiveGateway(pointer) && definition.getIncoming(pointer).size() == 1) {
                    GatewaySelector selector = new GatewaySelector();
                    GatewayMeta gw = definition.getGateways().get(pointer);
                    List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
                    List<String> chosenFlowIds = selector.chooseInclusive(gw, outgoing, variables);

                    for (String flowId : chosenFlowIds) {
                        queue.add(outgoing.stream().filter(f -> f.getId().equals(flowId)).findFirst().orElseThrow().getTargetRef());
                    }
                    String joinGatewayId = definition.findJoinGateway(pointer, GatewayMeta.Type.INCLUSIVE);
                    if (joinGatewayId != null) {
                        joinExpectedTokens.put(joinGatewayId, chosenFlowIds.size());
                        joinArrivedTokens.put(joinGatewayId, new HashSet<>());
                    }
                    current = null;
                }
                // JOINING GATEWAY (PARALLEL OR INCLUSIVE)
                else if ((definition.isParallelGateway(pointer) || definition.isInclusiveGateway(pointer)) && definition.getIncoming(pointer).size() > 1) {
                    int expected = joinExpectedTokens.getOrDefault(pointer, definition.getIncoming(pointer).size());
                    Set<String> arrived = joinArrivedTokens.computeIfAbsent(pointer, k -> new HashSet<>());
                    arrived.add(previousPointer);

                    if (arrived.size() >= expected) {
                        joinArrivedTokens.remove(pointer);
                        joinExpectedTokens.remove(pointer);
                        previousPointer = pointer;
                        current = definition.getOutgoing(pointer).get(0).getTargetRef();
                    } else {
                        current = null;
                    }
                }
                // END EVENT
                else if (definition.isEndEvent(pointer)) {
                    current = null;
                }
                // OTHER (Start, ServiceTask, etc.)
                else {
                    List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
                    previousPointer = pointer;
                    current = outgoing.isEmpty() ? null : outgoing.get(0).getTargetRef();
                }
            }
        }
        return newUserTasks;
    }
}
