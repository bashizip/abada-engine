package com.abada.engine.bpmn.compatibility;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class CompatibilityProfiles {
    public static final String STANDARD = "standard-bpmn-2.0";
    public static final String ABADA_NATIVE = "abada-native-1";
    public static final String CAMUNDA_7 = "camunda-7";
    public static final List<String> DEFAULT = List.of(STANDARD, ABADA_NATIVE, CAMUNDA_7);
    public static final Set<String> KNOWN = Set.copyOf(DEFAULT);

    private CompatibilityProfiles() {}

    public static List<String> validate(List<String> requested) {
        List<String> profiles = requested == null || requested.isEmpty() ? DEFAULT : requested;
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String profile : profiles) {
            if (!KNOWN.contains(profile)) {
                throw BpmnValidationException.single(new BpmnValidationIssue(
                        BpmnErrorCodes.UNKNOWN_PROFILE, ValidationSeverity.ERROR,
                        "Unknown compatibility profile '" + profile + "'", null, null, null, null,
                        "Use one of: " + String.join(", ", DEFAULT)));
            }
            result.add(profile);
        }
        if (!result.contains(STANDARD)) result.add(STANDARD);
        return List.copyOf(result);
    }
}
