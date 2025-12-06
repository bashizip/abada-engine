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

    // Error tracking for incident management
    @Lob
    @Column(name = "exception_message")
    private String exceptionMessage;

    @Lob
    @Column(name = "exception_stacktrace", columnDefinition = "TEXT")
    private String exceptionStacktrace;

    @Column(name = "retries")
    private Integer retries = 3; // Default retry count

    @Column(name = "activity_id")
    private String activityId; // BPMN element ID for visualization

    public enum Status {
        OPEN,
        LOCKED,
        FAILED // Added for failed jobs tracking
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

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getExceptionStacktrace() {
        return exceptionStacktrace;
    }

    public void setExceptionStacktrace(String exceptionStacktrace) {
        this.exceptionStacktrace = exceptionStacktrace;
    }

    public Integer getRetries() {
        return retries;
    }

    public void setRetries(Integer retries) {
        this.retries = retries;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }
}
