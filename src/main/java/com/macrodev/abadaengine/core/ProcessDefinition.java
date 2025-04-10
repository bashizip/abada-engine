package com.macrodev.abadaengine.core;

import com.macrodev.abadaengine.parser.BpmnParser;

import java.util.*;

/**
 * Represents a parsed BPMN process definition.
 * Contains metadata like process ID, name, and user tasks,
 * as well as a sequence map to determine flow transitions.

 * This class is used at runtime to instantiate and navigate
 * through BPMN-defined workflows.
 */
public class ProcessDefinition {

    private final String id;
    private final String name;
    private final String startEventId;
    private final Map<String, String> userTasks;
    private final Map<String, String> sequenceMap;

    public ProcessDefinition(String id, String name, String startEventId,
                             Map<String, String> userTasks,
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

    public String getTaskName(String taskId) {
        return userTasks.get(taskId);
    }

    public boolean isUserTask(String elementId) {
        return userTasks.containsKey(elementId);
    }

    public Set<String> getAllUserTaskIds() {
        return userTasks.keySet();
    }
}
