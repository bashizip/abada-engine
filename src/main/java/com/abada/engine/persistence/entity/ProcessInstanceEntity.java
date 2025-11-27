package com.abada.engine.persistence.entity;

import com.abada.engine.core.model.ProcessStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "process_instances")
public class ProcessInstanceEntity {

    @Id
    private String id;

    private String processDefinitionId;

    private String currentActivityId;

    @Enumerated(EnumType.STRING)
    private ProcessStatus status;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;

    @Column(name = "started_by", nullable = false)
    private String startedBy = "system";

    @Lob
    @Column(name = "variables_json", nullable = false)
    private String variablesJson; // Jackson-serialized Map

    // Constructors
    public ProcessInstanceEntity() {
    }

    public ProcessInstanceEntity(String id, String processDefinitionId, String currentActivityId,
            ProcessStatus status) {
        this.id = id;
        this.processDefinitionId = processDefinitionId;
        this.currentActivityId = currentActivityId;
        this.status = status;
    }

    public String getVariablesJson() {
        return variablesJson;
    }

    public void setVariablesJson(String variablesJson) {
        this.variablesJson = variablesJson;
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

    public ProcessStatus getStatus() {
        return status;
    }

    public void setStatus(ProcessStatus status) {
        this.status = status;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public String getStartedBy() {
        return startedBy;
    }

    public void setStartedBy(String startedBy) {
        this.startedBy = startedBy;
    }
}
