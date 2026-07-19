package com.abada.engine.bpmn.compatibility;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detects dialect namespaces without interpreting their execution semantics. */
public final class BpmnCompatibilityDetector {
    public static final String CAMUNDA_NAMESPACE = "http://camunda.org/schema/1.0/bpmn";
    public static final String ABADA_NAMESPACE = "https://abada.io/schema/bpmn";
    private static final Pattern XMLNS = Pattern.compile("\\bxmlns(?::[A-Za-z_][\\w.-]*)?\\s*=\\s*(['\"])(.*?)\\1");

    public Detection detect(String xml) {
        LinkedHashSet<String> namespaces = new LinkedHashSet<>();
        Matcher matcher = XMLNS.matcher(xml == null ? "" : xml);
        while (matcher.find()) namespaces.add(matcher.group(2));

        LinkedHashSet<String> profiles = new LinkedHashSet<>();
        profiles.add(CompatibilityProfiles.STANDARD);
        if (namespaces.contains(ABADA_NAMESPACE)) profiles.add(CompatibilityProfiles.ABADA_NATIVE);
        if (namespaces.contains(CAMUNDA_NAMESPACE)) profiles.add(CompatibilityProfiles.CAMUNDA_7);
        return new Detection(Set.copyOf(namespaces), Set.copyOf(profiles));
    }

    public record Detection(Set<String> namespaces, Set<String> profiles) {}
}
