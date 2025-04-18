package com.abada.engine.core;

import com.abada.engine.parser.BpmnParser;

import java.util.List;
import java.util.Map;

/**
 * Holds the metadata of a parsed BPMN process definition.
 */
public class ProcessDefinition {

    private final String id;
    private final String name;
    private final String startEventId;
    private final Map<String, BpmnParser.TaskMeta> userTasks;
    private final Map<String, String> sequenceFlows;

    public ProcessDefinition(String id, String name, String startEventId,
                             Map<String, BpmnParser.TaskMeta> userTasks,
                             List<BpmnParser.SequenceFlow> flows) {
        this.id = id;
        this.name = name;
        this.startEventId = startEventId;
        this.userTasks = userTasks;
        this.sequenceFlows = flows.stream()
                .collect(java.util.stream.Collectors.toMap(
                        BpmnParser.SequenceFlow::getFrom,
                        BpmnParser.SequenceFlow::getTo));
    }



    public String getId() {
        return id;
    }

    public String getStartEventId() {
        return startEventId;
    }

    public String getNextElement(String from) {
        return sequenceFlows.get(from);
    }

    public boolean isUserTask(String id) {
        return userTasks.containsKey(id);
    }

    public String getTaskName(String id) {
        return userTasks.get(id).name;
    }

    public String getTaskAssignee(String id) {
        return userTasks.get(id).assignee;
    }

    public List<String> getCandidateUsers(String id) {
        return userTasks.get(id).candidateUsers;
    }

    public List<String> getCandidateGroups(String id) {
        return userTasks.get(id).candidateGroups;
    }

    public String getName() {
        return name;
    }
}
