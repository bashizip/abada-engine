package com.abada.engine.parser.assignment;

import com.abada.engine.core.model.assignment.AssignmentStrategy;
import com.abada.engine.core.model.assignment.ProcessExpression;
import com.abada.engine.core.model.assignment.ProcessExpressions;
import com.abada.engine.core.model.assignment.UserTaskAssignment;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/** Translates the supported Camunda 7 assignment attributes at the parser boundary. */
public final class Camunda7AssignmentParser implements AssignmentDialectParser {
    @Override public String profileId() { return com.abada.engine.bpmn.compatibility.CompatibilityProfiles.CAMUNDA_7; }

    @Override
    public Optional<UserTaskAssignment> parse(UserTask task, AssignmentXml xml) {
        UserTaskAssignment assignment = parse(task);
        return assignment.equals(UserTaskAssignment.EMPTY) ? Optional.empty() : Optional.of(assignment);
    }

    public UserTaskAssignment parse(UserTask task) {
        Optional<ProcessExpression> assignee = optional(task.getCamundaAssignee());
        List<ProcessExpression> users = list(task.getCamundaCandidateUsers());
        List<ProcessExpression> groups = list(task.getCamundaCandidateGroups());
        AssignmentStrategy strategy = assignee.isPresent() ? AssignmentStrategy.DIRECT : AssignmentStrategy.CLAIM;
        return new UserTaskAssignment(assignee, users, groups, strategy);
    }

    private Optional<ProcessExpression> optional(String source) {
        return source == null || source.isBlank()
                ? Optional.empty()
                : Optional.of(ProcessExpressions.parse(source));
    }

    private List<ProcessExpression> list(String source) {
        if (source == null || source.isBlank()) return List.of();
        LinkedHashMap<String, ProcessExpression> normalized = new LinkedHashMap<>();
        for (String part : source.split(",", -1)) {
            if (part.isBlank()) continue;
            ProcessExpression expression = ProcessExpressions.parse(part);
            normalized.putIfAbsent(expression.source(), expression);
        }
        return new ArrayList<>(normalized.values());
    }
}
