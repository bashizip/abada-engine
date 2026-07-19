package com.abada.engine.bpmn.migration;

import com.abada.engine.bpmn.compatibility.CompatibilityReport;

public record BpmnMigrationResult(String originalXml, String migratedXml, CompatibilityReport report) {}
