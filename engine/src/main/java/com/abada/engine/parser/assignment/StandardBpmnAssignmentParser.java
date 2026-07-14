package com.abada.engine.parser.assignment;

import com.abada.engine.bpmn.compatibility.CompatibilityProfiles;
import com.abada.engine.core.model.assignment.*;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.*;

public final class StandardBpmnAssignmentParser implements AssignmentDialectParser {
    private static final String NS = "http://www.omg.org/spec/BPMN/20100524/MODEL";

    @Override public String profileId() { return CompatibilityProfiles.STANDARD; }

    @Override
    public Optional<UserTaskAssignment> parse(UserTask task, AssignmentXml xml) {
        Optional<Element> taskElement = xml.elementById(task.getId());
        if (taskElement.isEmpty()) return Optional.empty();
        Optional<ProcessExpression> assignee = Optional.empty();
        List<ProcessExpression> users = new ArrayList<>();
        List<ProcessExpression> groups = new ArrayList<>();

        for (Node child = taskElement.get().getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!(child instanceof Element role) || !NS.equals(role.getNamespaceURI())) continue;
            boolean performer = "humanPerformer".equals(role.getLocalName());
            boolean owner = "potentialOwner".equals(role.getLocalName());
            if (!performer && !owner) continue;
            String expression = formalExpression(role).orElseThrow(() ->
                    new IllegalArgumentException("Standard BPMN resource role requires a formalExpression"));
            int separator = expression.indexOf(':');
            if (separator <= 0 || separator == expression.length() - 1)
                throw new IllegalArgumentException("Unsupported standard BPMN resource expression '" + expression + "'");
            String kind = expression.substring(0, separator).trim();
            ProcessExpression value = ProcessExpressions.parse(expression.substring(separator + 1));
            if (performer) {
                if (!"user".equals(kind) || assignee.isPresent())
                    throw new IllegalArgumentException("humanPerformer supports exactly one user:<id> expression");
                assignee = Optional.of(value);
            } else if ("user".equals(kind)) users.add(value);
            else if ("group".equals(kind)) groups.add(value);
            else throw new IllegalArgumentException("Unsupported standard BPMN resource kind '" + kind + "'");
        }
        if (assignee.isEmpty() && users.isEmpty() && groups.isEmpty()) return Optional.empty();
        return Optional.of(new UserTaskAssignment(assignee, deduplicate(users), deduplicate(groups),
                assignee.isPresent() ? AssignmentStrategy.DIRECT : AssignmentStrategy.CLAIM));
    }

    private Optional<String> formalExpression(Element role) {
        return AssignmentXml.firstChild(role, NS, "resourceAssignmentExpression")
                .flatMap(expression -> AssignmentXml.firstChild(expression, NS, "formalExpression"))
                .map(Element::getTextContent).map(String::trim).filter(value -> !value.isEmpty());
    }

    private static List<ProcessExpression> deduplicate(List<ProcessExpression> values) {
        LinkedHashMap<String, ProcessExpression> result = new LinkedHashMap<>();
        values.forEach(value -> result.putIfAbsent(value.source(), value));
        return List.copyOf(result.values());
    }
}
