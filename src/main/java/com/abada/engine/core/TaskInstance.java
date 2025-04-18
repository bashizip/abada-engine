package com.abada.engine.core;

import java.util.List;

/**
 * Represents a user task instance in a process.
 */
public class TaskInstance {

    private final String taskId;
    private final String taskName;
    private final String processInstanceId;
    private String assignee;
    private final List<String> candidateUsers;
    private final List<String> candidateGroups;

    public TaskInstance(String taskId, String taskName, String processInstanceId,
                        String assignee, List<String> candidateUsers, List<String> candidateGroups) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.processInstanceId = processInstanceId;
        this.assignee = assignee;
        this.candidateUsers = candidateUsers;
        this.candidateGroups = candidateGroups;
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

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public List<String> getCandidateGroups() {
        return candidateGroups;
    }
}
