package com.abada.engine.bpmn.compatibility;

import java.util.List;
import java.util.Set;

public record CompatibilityReport(
        Set<String> detectedProfiles,
        List<CompatibilityMapping> mappings,
        List<BpmnValidationIssue> issues) {
    public CompatibilityReport {
        detectedProfiles = detectedProfiles == null ? Set.of() : Set.copyOf(detectedProfiles);
        mappings = mappings == null ? List.of() : List.copyOf(mappings);
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
    }
}
