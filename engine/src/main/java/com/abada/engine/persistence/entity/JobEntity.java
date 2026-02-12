package com.abada.engine.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class JobEntity {

    @Id
    private String id;

    private String processInstanceId;

    private String eventId;

    private Instant executionTimestamp;

    public JobEntity() {
        this.id = UUID.randomUUID().toString();
    }

    public JobEntity(String processInstanceId, String eventId, Instant executionTimestamp) {
        this();
        this.processInstanceId = processInstanceId;
        this.eventId = eventId;
        this.executionTimestamp = executionTimestamp;
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

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Instant getExecutionTimestamp() {
        return executionTimestamp;
    }

    public void setExecutionTimestamp(Instant executionTimestamp) {
        this.executionTimestamp = executionTimestamp;
    }
}
