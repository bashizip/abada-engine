package com.macrodev.abadaengine.core;

/**
 * Represents a user task assigned during a process execution.
 */
public class TaskInstance {

    private final String taskId; // ID of the BPMN user task
    private final String taskName; // Human-readable name of the task
    private final String processInstanceId; // ID of the process instance this task belongs to
    private final String assignee; // Assigned user (could be null if unassigned)

    public TaskInstance(String taskId, String taskName, String processInstanceId, String assignee) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.processInstanceId = processInstanceId;
        this.assignee = assignee;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getAssignee() {
        return assignee;
    }
}
