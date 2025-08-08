package com.abada.engine.core;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskInstance {

    private String id;
    private String processInstanceId;
    private String taskDefinitionKey;
    private String name;
    private String assignee;
    private boolean completed;
    private List<String> candidateUsers = new ArrayList<>();
    private List<String> candidateGroups = new ArrayList<>();

    public TaskInstance() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public void setCandidateUsers(List<String> candidateUsers) {
        // Ensure the internal list is never null to avoid NullPointerExceptions
        this.candidateUsers = candidateUsers != null ? candidateUsers : new ArrayList<>();
    }

    public List<String> getCandidateGroups() {
        return candidateGroups;
    }

    public void setCandidateGroups(List<String> candidateGroups) {
        // Ensure the internal list is never null to avoid NullPointerExceptions
        this.candidateGroups = candidateGroups != null ? candidateGroups : new ArrayList<>();
    }

    // Helper methods

    public boolean isClaimed() {
        return assignee != null && !assignee.isEmpty();
    }
}
