package com.abada.engine.bpmn.compatibility;

public record BpmnValidationIssue(
        String code,
        ValidationSeverity severity,
        String message,
        String processDefinitionId,
        String elementId,
        String namespace,
        SourceLocation sourceLocation,
        String suggestedResolution) {}
