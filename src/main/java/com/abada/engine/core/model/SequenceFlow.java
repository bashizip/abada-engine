package com.abada.engine.core.model;

import java.io.Serializable;

public class SequenceFlow implements Serializable {
    private final String id;
    private final String sourceRef;
    private final String targetRef;
    private final String name;
    private final String conditionExpression;

    public SequenceFlow(String id, String sourceRef, String targetRef) {
        this(id, sourceRef, targetRef, null, null);
    }

    public SequenceFlow(String id, String sourceRef, String targetRef, String name, String conditionExpression) {
        this.id = id;
        this.sourceRef = sourceRef;
        this.targetRef = targetRef;
        this.name = name;
        this.conditionExpression = conditionExpression;
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
}
