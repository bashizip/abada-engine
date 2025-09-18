package com.abada.engine.core.model;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class ParsedProcessDefinition implements Serializable {

    private final String id;
    private final String name;
    private final String startEventId;
    private final Map<String, TaskMeta> userTasks;
    private final List<SequenceFlow> sequenceFlows;
    private final Map<String, GatewayMeta> gateways;
    private final Map<String, EventMeta> events;
    private final Map<String, Object> endEvents;
    private final String rawXml;
    private final Map<String, List<SequenceFlow>> outgoingBySource = new HashMap<>();
    private final Map<String, List<SequenceFlow>> incomingByTarget = new HashMap<>();

    private final Map<String, List<String>> flowGraph = new HashMap<>();
    public List<SequenceFlow> getOutgoing(String sourceId) { return outgoingBySource.getOrDefault(sourceId, List.of()); }
    public List<SequenceFlow> getIncoming(String targetId) { return incomingByTarget.getOrDefault(targetId, List.of()); }


    public ParsedProcessDefinition(String id, String name, String startEventId,
                                   Map<String, TaskMeta> userTasks,
                                   List<SequenceFlow> sequenceFlows,
                                   Map<String, GatewayMeta> gateways,
                                   Map<String, EventMeta> events,
                                   Map<String, Object> endEvents,
                                   String rawXml) {
        this.id = id;
        this.name = name;
        this.startEventId = startEventId;
        this.userTasks = Collections.unmodifiableMap(new HashMap<>(userTasks));
        this.sequenceFlows = Collections.unmodifiableList(new ArrayList<>(sequenceFlows));
        this.gateways = Collections.unmodifiableMap(new HashMap<>(gateways));
        this.events = Collections.unmodifiableMap(new HashMap<>(events));
        this.endEvents = Collections.unmodifiableMap(new HashMap<>(endEvents));
        this.rawXml = rawXml;
        buildFlowGraph();
    }

    private void buildFlowGraph() {
        for (SequenceFlow flow : sequenceFlows) {
            flowGraph.computeIfAbsent(flow.getSourceRef(), k -> new ArrayList<>()).add(flow.getTargetRef());
            incomingByTarget.computeIfAbsent(flow.getTargetRef(), k -> new ArrayList<>()).add(flow);
        }
    }

    public String findJoinGateway(String forkGatewayId, GatewayMeta.Type forkGatewayType) {
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // Start traversal from all outgoing paths of the fork gateway
        for (SequenceFlow flow : getOutgoing(forkGatewayId)) {
            queue.add(flow.getTargetRef());
            visited.add(flow.getTargetRef());
        }

        while (!queue.isEmpty()) {
            String currentId = queue.poll();
            GatewayMeta currentGw = gateways.get(currentId);

            // If we find a gateway of the same type with more than one incoming path, it's our join gateway
            if (currentGw != null && currentGw.type() == forkGatewayType && getIncoming(currentId).size() > 1) {
                return currentId;
            }

            // Continue traversal
            for (SequenceFlow outgoingFlow : getOutgoing(currentId)) {
                String nextId = outgoingFlow.getTargetRef();
                if (!visited.contains(nextId)) {
                    queue.add(nextId);
                    visited.add(nextId);
                }
            }
        }

        return null; // No join gateway found
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStartEventId() {
        return startEventId;
    }

    public Map<String, TaskMeta> getUserTasks() {
        return userTasks;
    }

    public List<SequenceFlow> getSequenceFlows() {
        return sequenceFlows;
    }

    public Map<String, GatewayMeta> getGateways() {
        return gateways;
    }

    public Map<String, EventMeta> getEvents() {
        return events;
    }

    public String getRawXml() {
        return rawXml;
    }

    public TaskMeta getUserTask(String taskId) {
        return userTasks.get(taskId);
    }

    public List<String> getNextActivities(String fromId) {
        return flowGraph.getOrDefault(fromId, Collections.emptyList());
    }

    public String getNextActivity(String fromId) {
        List<String> next = getNextActivities(fromId);
        return next.isEmpty() ? null : next.get(0);
    }

    public List<SequenceFlow> getOutgoingFlows(String sourceId) {
        return sequenceFlows.stream()
                .filter(flow -> flow.getSourceRef().equals(sourceId))
                .collect(Collectors.toList());
    }

    public boolean isEnd(String activityId) {
        return getNextActivity(activityId).isEmpty();
    }

    public Set<String> getAllActivityIds() {
        Set<String> ids = new HashSet<>();
        ids.addAll(userTasks.keySet());
        ids.addAll(flowGraph.keySet());
        flowGraph.values().forEach(ids::addAll);
        return ids;
    }

    public boolean isUserTask(String id) {
        return userTasks.containsKey(id);
    }

    public boolean isGateway(String id) {
        return gateways.containsKey(id);
    }

    public boolean isCatchEvent(String id) {
        return events.containsKey(id);
    }


    // ==========================================================
    // Typed gateway helpers (Exclusive, Inclusive, Parallel)
    // ==========================================================

    /** Returns true if the node is an Exclusive Gateway. */
    public boolean isExclusiveGateway(String activityId) {
        GatewayMeta gw = gateways.get(activityId);
        return gw != null && gw.type() == GatewayMeta.Type.EXCLUSIVE;
    }

    /** Returns true if the node is an Inclusive Gateway. */
    public boolean isInclusiveGateway(String activityId) {
        GatewayMeta gw = gateways.get(activityId);
        return gw != null && gw.type() == GatewayMeta.Type.INCLUSIVE;
    }

    /** Returns true if the node is a Parallel Gateway. */
    public boolean isParallelGateway(String activityId) {
        GatewayMeta gw = gateways.get(activityId);
        return gw != null && gw.type() == GatewayMeta.Type.PARALLEL;
    }

    // Convenience: create/update API the parser can call when discovering gateways
    public void putGateway(String id, GatewayMeta meta) {
        gateways.put(id, meta);
    }

    // Convenience: outgoing registration used by the parser
    public void addOutgoing(String sourceId, SequenceFlow flow) {
        outgoingBySource.computeIfAbsent(sourceId, k -> new ArrayList<>()).add(flow);
    }


    public GatewayMeta getGateway(String id) {
        return gateways.get(id);
    }

    public boolean isEndEvent(String id) {
        return endEvents.containsKey(id);
    }

    public String getTaskName(String id) {
        TaskMeta meta = userTasks.get(id);
        return meta != null ? meta.getName() : null;
    }

    public String getTaskAssignee(String taskId) {
        TaskMeta meta = userTasks.get(taskId);
        return meta != null ? meta.getAssignee() : null;
    }

    public List<String> getCandidateUsers(String taskId) {
        TaskMeta meta = userTasks.get(taskId);
        return meta != null ? meta.getCandidateUsers() : List.of();
    }

    public List<String> getCandidateGroups(String taskId) {
        TaskMeta meta = userTasks.get(taskId);
        return meta != null ? meta.getCandidateGroups() : List.of();
    }
}
