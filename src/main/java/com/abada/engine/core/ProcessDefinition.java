package com.abada.engine.core;

import java.util.*;
import com.abada.engine.parser.BpmnParser;

public class ProcessDefinition {

    private final String id;
    private final String name;
    private final String startEventId;
    private final Map<String, BpmnParser.TaskMeta> userTasks;
    private final Map<String, String> sequenceMap;

    public ProcessDefinition(String id, String name, String startEventId,
                             Map<String, BpmnParser.TaskMeta> userTasks,
                             List<BpmnParser.SequenceFlow> flows) {
        this.id = id;
        this.name = name;
        this.startEventId = startEventId;
        this.userTasks = userTasks;
        this.sequenceMap = new HashMap<>();
        for (BpmnParser.SequenceFlow flow : flows) {
            sequenceMap.put(flow.sourceRef, flow.targetRef);
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

    public String getNextElement(String currentElementId) {
        return sequenceMap.get(currentElementId);
    }

    public boolean isUserTask(String elementId) {
        return userTasks.containsKey(elementId);
    }

    public String getTaskName(String taskId) {
        return userTasks.get(taskId).name;
    }

    public String getTaskAssignee(String taskId) {
        return userTasks.get(taskId).assignee;
    }

    public List<String> getCandidateUsers(String taskId) {
        return userTasks.get(taskId).candidateUsers;
    }

    public List<String> getCandidateGroups(String taskId) {
        return userTasks.get(taskId).candidateGroups;
    }

    public Set<String> getAllUserTaskIds() {
        return userTasks.keySet();
    }
}
