package com.abada.engine.bpmn.compatibility;

import com.abada.engine.core.model.ParsedProcessDefinition;
import java.util.List;
import java.util.Set;

public record BpmnParseResult(
        ParsedProcessDefinition definition,
        CompatibilityReport report,
        List<String> activeProfiles,
        Set<String> detectedNamespaces) {}
