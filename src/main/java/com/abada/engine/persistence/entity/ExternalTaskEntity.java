package com.abada.engine.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "external_tasks")
public class ExternalTaskEntity {

    @Id
    private String id;

    private String processInstanceId;

    private String topicName;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String workerId; // The ID of the worker that has locked the task

    private Instant lockExpirationTime;

    public enum Status {
        OPEN,
        LOCKED
    }

    public ExternalTaskEntity() {
        this.id = UUID.randomUUID().toString();
        this.status = Status.OPEN;
    }

    public ExternalTaskEntity(String processInstanceId, String topicName) {
        this();
        this.processInstanceId = processInstanceId;
        this.topicName = topicName;
    }

    // Getters and Setters

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

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Instant getLockExpirationTime() {
        return lockExpirationTime;
    }

    public void setLockExpirationTime(Instant lockExpirationTime) {
        this.lockExpirationTime = lockExpirationTime;
    }
}
