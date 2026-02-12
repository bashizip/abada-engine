package com.abada.engine.core.model;

import java.io.Serializable;

public class SequenceFlow implements Serializable {
    private final String id;
    private final String sourceRef;
    private final String targetRef;
    private final String name;
    private final String conditionExpression;
    private final boolean isDefault;
    private String language; // (optional: to support expression languages like groovy, js, etc.)


    public SequenceFlow(String id, String sourceRef, String targetRef,
                        String name, String conditionExpression, boolean isDefault) {
        this.id = id;
        this.sourceRef = sourceRef;
        this.targetRef = targetRef;
        this.name = name;
        this.conditionExpression = conditionExpression;
        this.isDefault = isDefault;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public String getId() {
        return id;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getTargetRef() {
        return targetRef;
    }

    public String getName() {
        return name;
    }

    public String getConditionExpression() {
        return conditionExpression;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
