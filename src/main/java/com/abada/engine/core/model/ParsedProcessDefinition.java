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
    private final String rawXml;

    private final Map<String, List<String>> flowGraph = new HashMap<>();

    public ParsedProcessDefinition(String id, String name, String startEventId,
                                   Map<String, TaskMeta> userTasks,
                                   List<SequenceFlow> sequenceFlows,
                                   String rawXml) {
        this.id = id;
        this.name = name;
        this.startEventId = startEventId;
        this.userTasks = Collections.unmodifiableMap(new HashMap<>(userTasks));
        this.sequenceFlows = Collections.unmodifiableList(new ArrayList<>(sequenceFlows));
        this.rawXml = rawXml;
        buildFlowGraph();
    }

    private void buildFlowGraph() {
        for (SequenceFlow flow : sequenceFlows) {
            flowGraph.computeIfAbsent(flow.getSourceRef(), k -> new ArrayList<>())
                    .add(flow.getTargetRef());
        }
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

    public String getTaskName(String id) {
        TaskMeta meta = userTasks.get(id);
        return meta != null ? meta.name : null;
    }

    public String getTaskAssignee(String taskId) {
        TaskMeta meta = userTasks.get(taskId);
        return meta != null ? meta.assignee : null;
    }

    public List<String> getCandidateUsers(String taskId) {
        TaskMeta meta = userTasks.get(taskId);
        return meta != null ? meta.candidateUsers : List.of();
    }

    public List<String> getCandidateGroups(String taskId) {
        TaskMeta meta = userTasks.get(taskId);
        return meta != null ? meta.candidateGroups : List.of();
    }
}
