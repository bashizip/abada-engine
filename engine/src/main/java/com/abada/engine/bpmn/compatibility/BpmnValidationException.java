package com.abada.engine.bpmn.compatibility;

import com.abada.engine.core.exception.ProcessEngineException;
import java.util.List;

public final class BpmnValidationException extends ProcessEngineException {
    private final List<BpmnValidationIssue> issues;

    public BpmnValidationException(List<BpmnValidationIssue> issues) {
        super(issues == null || issues.isEmpty() ? "BPMN validation failed"
                : issues.get(0).code() + ": " + issues.get(0).message());
        this.issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public static BpmnValidationException single(BpmnValidationIssue issue) {
        return new BpmnValidationException(List.of(issue));
    }

    public List<BpmnValidationIssue> getIssues() { return issues; }
}
