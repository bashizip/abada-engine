package com.abada.engine.persistence.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
public class TaskEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String processInstanceId;

    @Column
    private String assignee;

    @Column
    private String taskDefinitionKey;

    @Column
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_candidate_users", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "user_id")
    private List<String> candidateUsers = new ArrayList<>();


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "task_candidate_groups", joinColumns = @JoinColumn(name = "task_id"))
    @Column(name = "group_id")
    private List<String> candidateGroups = new ArrayList<>();


    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        CREATED,     // Task created, not yet assigned or claimed
        ASSIGNED,    // Task has been claimed by a user
        COMPLETED,   // Task completed successfully
        CANCELLED    // Task was cancelled (optional for admin control)
    }

    public Status getStatus() {
        return status;
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

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
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
}