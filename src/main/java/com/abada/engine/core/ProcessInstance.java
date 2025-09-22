package com.abada.engine.core;

import com.abada.engine.core.model.*;
import com.abada.engine.dto.UserTaskPayload;
import com.abada.engine.spi.DelegateExecution;
import com.abada.engine.spi.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public class ProcessInstance {

    private String id;
    private ParsedProcessDefinition definition;
    private final Map<String, Object> variables = new HashMap<>();
    private Instant startDate;
    private Instant endDate;

    private final List<String> activeTokens = new ArrayList<>();
    private final Map<String, Integer> joinExpectedTokens = new HashMap<>();
    private final Map<String, Set<String>> joinArrivedTokens = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(ProcessInstance.class);

    public ProcessInstance(ParsedProcessDefinition definition) {
        this.id = UUID.randomUUID().toString();
        this.definition = definition;
        this.activeTokens.add(definition.getStartEventId());
        this.startDate = Instant.now();
    }

    public ProcessInstance(String id, ParsedProcessDefinition definition, List<String> activeTokens, Instant startDate, Instant endDate) {
        this.id = id;
        this.definition = definition;
        this.activeTokens.addAll(activeTokens);
        this.startDate = startDate;
        this.endDate = endDate;
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

    public Instant getStartDate() {
        return startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public void setVariable(String key, Object value) { variables.put(key, value); }
    public Object getVariable(String key) { return variables.get(key); }
    public Map<String, Object> getVariables() { return Collections.unmodifiableMap(variables); }
    public void putAllVariables(Map<String,Object> newVars) {
        if (newVars != null) variables.putAll(newVars);
    }

    public boolean isWaitingForUserTask() {
        return !activeTokens.isEmpty() && activeTokens.stream().anyMatch(t -> definition.isUserTask(t));
    }

    public boolean isCompleted() { return activeTokens.isEmpty(); }

    public List<UserTaskPayload> advance() {
        return advance(null);
    }

    public List<UserTaskPayload> advance(String resumedNodeId) {
        List<UserTaskPayload> newUserTasks = new ArrayList<>();
        Queue<String> queue = new LinkedList<>();

        if (resumedNodeId != null) {
            activeTokens.remove(resumedNodeId);
            queue.add(resumedNodeId);
        } else {
            queue.addAll(activeTokens);
            activeTokens.clear();
        }

        Set<String> processedInThisRun = new HashSet<>();

        while (!queue.isEmpty()) {
            String current = queue.poll();
            String previousPointer = null;

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

                ServiceTaskMeta serviceTaskMeta = definition.getServiceTask(pointer);
                boolean isExternalServiceTask = serviceTaskMeta != null && serviceTaskMeta.topicName() != null;
                boolean isEmbeddedServiceTask = serviceTaskMeta != null && serviceTaskMeta.className() != null;

                // Is it a wait state? (User Task, Catch Event, or External Service Task)
                if (definition.isUserTask(pointer) || definition.isCatchEvent(pointer) || isExternalServiceTask) {
                    if (Objects.equals(pointer, resumedNodeId)) {
                        // This node was just completed or triggered, so advance from it.
                        var outgoing = definition.getOutgoing(pointer);
                        current = outgoing.isEmpty() ? null : outgoing.get(0).getTargetRef();
                        previousPointer = pointer;
                    } else {
                        // Arrived at a new wait state. Add to active tokens and stop this path.
                        activeTokens.add(pointer);
                        if (definition.isUserTask(pointer)) {
                            TaskMeta ut = definition.getUserTask(pointer);
                            newUserTasks.add(new UserTaskPayload(ut.getId(), ut.getName(), ut.getAssignee(), ut.getCandidateUsers(), ut.getCandidateGroups()));
                        }
                        current = null;
                    }
                } 
                // Is it an embedded, synchronous service task?
                else if (isEmbeddedServiceTask) {
                    try {
                        JavaDelegate delegate = (JavaDelegate) Class.forName(serviceTaskMeta.className()).getConstructor().newInstance();
                        delegate.execute(new DelegateExecutionImpl());
                        previousPointer = pointer;
                        List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
                        current = outgoing.isEmpty() ? null : outgoing.get(0).getTargetRef();
                    } catch (Exception e) {
                        throw new RuntimeException("Error executing JavaDelegate " + serviceTaskMeta.className(), e);
                    }
                } else if (definition.isExclusiveGateway(pointer)) {
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
                } else if (definition.isParallelGateway(pointer) && definition.getIncoming(pointer).size() == 1) {
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
                } else if (definition.isInclusiveGateway(pointer) && definition.getIncoming(pointer).size() == 1) {
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
                } else if ((definition.isParallelGateway(pointer) || definition.isInclusiveGateway(pointer)) && definition.getIncoming(pointer).size() > 1) {
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
                } else if (definition.isEndEvent(pointer)) {
                    current = null;
                } else {
                    List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
                    previousPointer = pointer;
                    current = outgoing.isEmpty() ? null : outgoing.get(0).getTargetRef();
                }
            }
        }
        return newUserTasks;
    }

    private class DelegateExecutionImpl implements DelegateExecution {
        @Override
        public String getProcessInstanceId() {
            return ProcessInstance.this.id;
        }

        @Override
        public Map<String, Object> getVariables() {
            return Collections.unmodifiableMap(ProcessInstance.this.variables);
        }

        @Override
        public Object getVariable(String name) {
            return ProcessInstance.this.variables.get(name);
        }

        @Override
        public void setVariable(String name, Object value) {
            ProcessInstance.this.variables.put(name, value);
        }
    }
}
