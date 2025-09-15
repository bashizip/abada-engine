package com.abada.engine.core;

import com.abada.engine.core.model.GatewayMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.SequenceFlow;
import com.abada.engine.core.model.TaskMeta;
import com.abada.engine.dto.UserTaskPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProcessInstance {

    private String id;
    private ParsedProcessDefinition definition;
    private final Map<String, Object> variables = new HashMap<>();

    // List of active execution pointers (activity IDs)
    private final List<String> activeTokens = new ArrayList<>();

    // Tracks how many tokens are expected at a joining gateway
    // Key: Gateway ID, Value: Number of expected tokens
    private final Map<String, Integer> joinExpectedTokens = new HashMap<>();

    // Tracks how many tokens have arrived at a joining gateway
    // Key: Gateway ID, Value: Set of arrived token (source activity) IDs
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

    /**
     * Advance execution until the next external wait state (User Task) or End Event.
     * Returns the next user task payload (to be created by TaskManager) if any.
     */
    public List<UserTaskPayload> advance() {
        // Default behaviour: stop when we REACH a user task (used by engine.start)
        return advance(null);
    }


    public List<UserTaskPayload> advance(String completedUserTask) {
        List<UserTaskPayload> newUserTasks = new ArrayList<>();
        Queue<String> queue = new LinkedList<>(activeTokens);
        activeTokens.clear(); // Clear current tokens, they will be advanced

        Set<String> processedInThisRun = new HashSet<>();

        if (completedUserTask != null) {
            queue.add(completedUserTask);
        }

        while (!queue.isEmpty()) {
            String current = queue.poll();
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
                        // This task was just completed, so we move on
                        var outgoing = definition.getOutgoing(pointer);
                        if (outgoing.isEmpty()) {
                            current = null; // End of path
                        } else {
                            current = outgoing.get(0).getTargetRef();
                        }
                    } else {
                        // This is a new wait state
                        activeTokens.add(pointer);
                        TaskMeta ut = definition.getUserTask(pointer);
                        newUserTasks.add(new UserTaskPayload(ut.getId(), ut.getName(), ut.getAssignee(), ut.getCandidateUsers(), ut.getCandidateGroups()));
                        current = null; // Stop this path
                    }
                }
                // EXCLUSIVE GATEWAY
                else if (definition.isExclusiveGateway(pointer)) {
                    GatewaySelector selector = new GatewaySelector();
                    GatewayMeta gw = definition.getGateways().get(pointer);
                    List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
                    String chosenFlowId = selector.chooseOutgoing(gw, outgoing, variables);
                    current = outgoing.stream()
                            .filter(f -> Objects.equals(f.getId(), chosenFlowId))
                            .map(SequenceFlow::getTargetRef)
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Flow not found: " + chosenFlowId));
                }
                // INCLUSIVE GATEWAY (FORK)
                else if (definition.isInclusiveGateway(pointer) && definition.getIncoming(pointer).size() == 1) {
                    GatewaySelector selector = new GatewaySelector();
                    GatewayMeta gw = definition.getGateways().get(pointer);
                    List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
                    List<String> chosenFlowIds = selector.chooseInclusive(gw, outgoing, variables);

                    // For each chosen path, add the target to the queue for processing
                    for (String flowId : chosenFlowIds) {
                        String target = outgoing.stream()
                                .filter(f -> Objects.equals(f.getId(), flowId))
                                .map(SequenceFlow::getTargetRef)
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException("Flow not found: " + flowId));
                        queue.add(target);
                    }

                    // Set up expectation for the corresponding join gateway
                    String joinGatewayId = definition.findJoinGateway(pointer);
                    if (joinGatewayId != null) {
                        joinExpectedTokens.put(joinGatewayId, chosenFlowIds.size());
                        joinArrivedTokens.put(joinGatewayId, new HashSet<>());
                    }

                    current = null; // Stop this path, new paths are in the queue
                }
                // INCLUSIVE GATEWAY (JOIN)
                else if (definition.isInclusiveGateway(pointer) && definition.getIncoming(pointer).size() > 1) {
                    int expected = joinExpectedTokens.getOrDefault(pointer, 0);
                    Set<String> arrived = joinArrivedTokens.computeIfAbsent(pointer, k -> new HashSet<>());
                    arrived.add(current); // Assuming 'current' is the ID of the activity leading to the join

                    if (arrived.size() >= expected) {
                        // All paths have arrived, we can proceed
                        joinArrivedTokens.remove(pointer);
                        joinExpectedTokens.remove(pointer);
                        current = definition.getOutgoing(pointer).get(0).getTargetRef();
                    } else {
                        // Not all paths have arrived, wait
                        current = null;
                    }
                }
                // END EVENT
                else if (definition.isEndEvent(pointer)) {
                    current = null; // End of this path
                }
                // OTHER (Start, ServiceTask, etc.)
                else {
                    List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
                    if (outgoing.isEmpty()) {
                        current = null; // End of this path
                    } else {
                        // Follow the first (and only) path
                        current = outgoing.get(0).getTargetRef();
                    }
                }
            }
        }
        return newUserTasks;
    }
}