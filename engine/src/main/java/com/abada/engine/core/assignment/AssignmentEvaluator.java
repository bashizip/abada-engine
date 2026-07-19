package com.abada.engine.core.assignment;

import com.abada.engine.core.exception.ProcessEngineException;
import com.abada.engine.core.model.assignment.*;

import java.util.*;

/** Evaluates canonical assignment expressions once, when a task instance is created. */
public final class AssignmentEvaluator {
    public ResolvedAssignment evaluate(UserTaskAssignment assignment, Map<String, Object> variables) {
        List<String> assignees = assignment.assignee().map(value -> resolve(value, variables)).orElse(List.of());
        if (assignees.size() > 1) throw new ProcessEngineException("Assignee expression resolved to multiple users");
        String assignee = assignees.isEmpty() ? null : assignees.getFirst();
        return new ResolvedAssignment(assignee, resolveAll(assignment.candidateUsers(), variables),
                resolveAll(assignment.candidateGroups(), variables), assignment.strategy());
    }

    private List<String> resolveAll(List<ProcessExpression> expressions, Map<String, Object> variables) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        expressions.forEach(expression -> values.addAll(resolve(expression, variables)));
        return List.copyOf(values);
    }

    private List<String> resolve(ProcessExpression expression, Map<String, Object> variables) {
        Object value = expression instanceof LiteralExpression literal ? literal.source()
                : lookup(((DynamicExpression) expression).source(), variables);
        if (value == null) return List.of();
        Collection<?> values = value instanceof Collection<?> collection ? collection : List.of(value);
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (Object item : values) if (item != null && !String.valueOf(item).isBlank())
            normalized.add(String.valueOf(item).trim());
        return List.copyOf(normalized);
    }

    private Object lookup(String source, Map<String, Object> variables) {
        String path = source.substring(2, source.length() - 1).trim();
        Object current = variables;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = map.get(part);
        }
        return current;
    }

    public record ResolvedAssignment(String assignee, List<String> candidateUsers,
            List<String> candidateGroups, AssignmentStrategy strategy) {}
}
