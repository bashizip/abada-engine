package com.abada.engine.bpmn.compatibility;

import java.util.List;

public record BpmnParseOptions(List<String> compatibilityProfiles, boolean rejectVendorExtensions, boolean strict) {
    public BpmnParseOptions {
        compatibilityProfiles = CompatibilityProfiles.validate(compatibilityProfiles);
    }
    public static BpmnParseOptions defaults() { return new BpmnParseOptions(CompatibilityProfiles.DEFAULT, false, false); }
}
