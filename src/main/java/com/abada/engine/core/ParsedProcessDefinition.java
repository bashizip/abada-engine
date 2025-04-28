

package com.abada.engine.core;

import com.abada.engine.parser.BpmnParser;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Holds the metadata of a parsed BPMN process definition.
 */
public class ParsedProcessDefinition {

    private final String id;
    private final String name;
    private final String startEventId;
    private final Map<String, BpmnParser.TaskMeta> userTasks;
    private final Map<String, String> sequenceFlows;
    private final String bpmnXml; // Added for persistence

    public ParsedProcessDefinition(String id, String name, String startEventId,
                                   Map<String, BpmnParser.TaskMeta> userTasks,
                                   List<BpmnParser.SequenceFlow> flows,
                                   String bpmnXml) {
        this.id = id;
        this.name = name;
        this.startEventId = startEventId;
        this.userTasks = userTasks;
        this.sequenceFlows = flows.stream()
                .collect(Collectors.toMap(BpmnParser.SequenceFlow::getFrom, BpmnParser.SequenceFlow::getTo));
        this.bpmnXml = bpmnXml;
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


    public boolean isUserTask(String id) {
        return userTasks.containsKey(id);
    }

    public String getTaskName(String id) {
        return userTasks.get(id).name;
    }

    /**
     * Returns the next element ID in the sequence after the given element.
     *
     * @param currentElementId ID of the current BPMN element.
     * @return ID of the next BPMN element, or null if none found.
     */
    public String getNextActivity(String currentElementId) {
        return sequenceFlows.get(currentElementId);
    }


    /**
     * Returns the assignee of the given task ID if defined.
     */
    public String getTaskAssignee(String taskId) {
        BpmnParser.TaskMeta meta = userTasks.get(taskId);
        return meta != null ? meta.assignee : null;
    }

    /**
     * Returns the candidate users list for the given task ID.
     */
    public List<String> getCandidateUsers(String taskId) {
        BpmnParser.TaskMeta meta = userTasks.get(taskId);
        return meta != null ? meta.candidateUsers : List.of();
    }

    /**
     * Returns the candidate groups list for the given task ID.
     */
    public List<String> getCandidateGroups(String taskId) {
        BpmnParser.TaskMeta meta = userTasks.get(taskId);
        return meta != null ? meta.candidateGroups : List.of();
    }

    public String getBpmnXml() {
        return bpmnXml;
    }

    public Map<String, BpmnParser.TaskMeta> getUserTasks() {
        return userTasks;
    }

    public Map<String, String> getSequenceFlows() {
        return sequenceFlows;
    }
}
