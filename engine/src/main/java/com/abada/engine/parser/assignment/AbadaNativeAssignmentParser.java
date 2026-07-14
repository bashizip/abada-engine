package com.abada.engine.parser.assignment;

import com.abada.engine.bpmn.compatibility.BpmnCompatibilityDetector;
import com.abada.engine.bpmn.compatibility.CompatibilityProfiles;
import com.abada.engine.core.model.assignment.*;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.*;

public final class AbadaNativeAssignmentParser implements AssignmentDialectParser {
    private static final String NS = BpmnCompatibilityDetector.ABADA_NAMESPACE;

    @Override public String profileId() { return CompatibilityProfiles.ABADA_NATIVE; }

    @Override
    public Optional<UserTaskAssignment> parse(UserTask task, AssignmentXml xml) {
        Optional<Element> taskElement = xml.elementById(task.getId());
        if (taskElement.isEmpty()) return Optional.empty();
        Optional<Element> extensions = AssignmentXml.firstChild(taskElement.get(),
                "http://www.omg.org/spec/BPMN/20100524/MODEL", "extensionElements");
        if (extensions.isEmpty()) return Optional.empty();
        Optional<Element> assignment = AssignmentXml.firstChild(extensions.get(), NS, "assignment");
        if (assignment.isEmpty()) return Optional.empty();

        Element source = assignment.get();
        Optional<ProcessExpression> assignee = expressionAttribute(source, "assignee");
        List<ProcessExpression> users = listAttribute(source, "candidateUsers");
        List<ProcessExpression> groups = listAttribute(source, "candidateGroups");

        for (Node child = source.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element element) || !NS.equals(element.getNamespaceURI())) continue;
            if ("assignee".equals(element.getLocalName())) assignee = Optional.of(elementExpression(element));
            if ("candidateUsers".equals(element.getLocalName())) users = nested(element, "user");
            if ("candidateGroups".equals(element.getLocalName())) groups = nested(element, "group");
        }
        AssignmentStrategy strategy = parseStrategy(source.getAttribute("strategy"), assignee.isPresent());
        return Optional.of(new UserTaskAssignment(assignee, users, groups, strategy));
    }

    private static AssignmentStrategy parseStrategy(String value, boolean assigned) {
        if (value == null || value.isBlank()) return assigned ? AssignmentStrategy.DIRECT : AssignmentStrategy.CLAIM;
        try { return AssignmentStrategy.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Invalid assignment strategy '" + value + "'"); }
    }

    private static Optional<ProcessExpression> expressionAttribute(Element element, String name) {
        return element.hasAttribute(name) && !element.getAttribute(name).isBlank()
                ? Optional.of(ProcessExpressions.parse(element.getAttribute(name))) : Optional.empty();
    }

    private static List<ProcessExpression> listAttribute(Element element, String name) {
        return element.hasAttribute(name) ? split(element.getAttribute(name)) : List.of();
    }

    private static List<ProcessExpression> split(String value) {
        LinkedHashMap<String, ProcessExpression> result = new LinkedHashMap<>();
        for (String part : value.split(",", -1)) if (!part.isBlank()) {
            ProcessExpression expression = ProcessExpressions.parse(part);
            result.putIfAbsent(expression.source(), expression);
        }
        return List.copyOf(result.values());
    }

    private static List<ProcessExpression> nested(Element parent, String localName) {
        List<ProcessExpression> values = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element && NS.equals(element.getNamespaceURI())
                    && localName.equals(element.getLocalName())) values.add(elementExpression(element));
        }
        return values.stream().collect(java.util.stream.Collectors.collectingAndThen(
                java.util.stream.Collectors.toMap(ProcessExpression::source, value -> value, (a, b) -> a, LinkedHashMap::new),
                map -> List.copyOf(map.values())));
    }

    private static ProcessExpression elementExpression(Element element) {
        String value = element.hasAttribute("expression") ? element.getAttribute("expression") : element.getAttribute("value");
        return ProcessExpressions.parse(value);
    }
}
