package com.abada.engine.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "process_definitions")
public class ProcessDefinitionEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String documentation;

    @Column(columnDefinition = "TEXT")
    private String bpmnXml;

    @Column(name = "candidate_starter_groups")
    private String candidateStarterGroups;

    @Column(name = "candidate_starter_users")
    private String candidateStarterUsers;

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

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

    public String getBpmnXml() {
        return bpmnXml;
    }

    public void setBpmnXml(String bpmnXml) {
        this.bpmnXml = bpmnXml;
    }

    public String getCandidateStarterGroups() {
        return candidateStarterGroups;
    }

    public void setCandidateStarterGroups(String candidateStarterGroups) {
        this.candidateStarterGroups = candidateStarterGroups;
    }

    public String getCandidateStarterUsers() {
        return candidateStarterUsers;
    }

    public void setCandidateStarterUsers(String candidateStarterUsers) {
        this.candidateStarterUsers = candidateStarterUsers;
    }

}
