package com.abada.engine.runtime;

public class DeployedProcessDefinition {

    private String id;
    private String name;
    private String bpmnXml;

    public DeployedProcessDefinition(String id, String name, String bpmnXml) {
        this.id = id;
        this.name = name;
        this.bpmnXml = bpmnXml;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBpmnXml() {
        return bpmnXml;
    }
}
