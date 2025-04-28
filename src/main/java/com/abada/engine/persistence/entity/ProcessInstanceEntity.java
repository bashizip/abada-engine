package com.abada.engine.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "process_instances")
public class ProcessInstanceEntity {

    @Id
    private String id;

    private String processDefinitionId;

    private String currentActivityId;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        RUNNING,
        WAITING_USER,
        COMPLETED,
        SUSPENDED,
        CANCELLED
    }

    // Constructors
    public ProcessInstanceEntity() {
    }

    public ProcessInstanceEntity(String id, String processDefinitionId, String currentActivityId, Status status) {
        this.id = id;
        this.processDefinitionId = processDefinitionId;
        this.currentActivityId = currentActivityId;
        this.status = status;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getCurrentActivityId() {
        return currentActivityId;
    }

    public void setCurrentActivityId(String currentActivityId) {
        this.currentActivityId = currentActivityId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
