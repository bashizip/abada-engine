package com.abada.engine.core.model;

import java.io.Serializable;
import java.util.List;

public class TaskMeta implements Serializable {
    private String id;
    private String name;
    private String assignee;
    private List<String> candidateUsers;
    private List<String> candidateGroups;
    private String formKey;
    private String dueDate;
    private String followUpDate;
    private String priority;
    private String documentation;

    // Future fields (e.g., listeners, multi-instance, conditions) can be added here.


    public TaskMeta() {

    }

    public TaskMeta(String id, String name, String assignee, List<String> candidateUsers, List<String> candidateGroups, String formKey, String dueDate, String followUpDate, String priority, String documentation) {
        this.id = id;
        this.name = name;
        this.assignee = assignee;
        this.candidateUsers = candidateUsers;
        this.candidateGroups = candidateGroups;
        this.formKey = formKey;
        this.dueDate = dueDate;
        this.followUpDate = followUpDate;
        this.priority = priority;
        this.documentation = documentation;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public void setCandidateUsers(List<String> candidateUsers) {
        this.candidateUsers = candidateUsers;
    }

    public List<String> getCandidateGroups() {
        return candidateGroups;
    }

    public void setCandidateGroups(List<String> candidateGroups) {
        this.candidateGroups = candidateGroups;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getFollowUpDate() {
        return followUpDate;
    }

    public void setFollowUpDate(String followUpDate) {
        this.followUpDate = followUpDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }
}
