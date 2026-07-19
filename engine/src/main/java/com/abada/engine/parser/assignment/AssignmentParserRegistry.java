package com.abada.engine.parser.assignment;

import com.abada.engine.bpmn.compatibility.*;
import com.abada.engine.core.model.assignment.UserTaskAssignment;
import org.camunda.bpm.model.bpmn.instance.UserTask;

import java.util.ArrayList;
import java.util.List;

/** Deterministic standard, native, then compatibility parser ordering. */
public final class AssignmentParserRegistry {
    private final List<AssignmentDialectParser> parsers = List.of(
            new StandardBpmnAssignmentParser(), new AbadaNativeAssignmentParser(), new Camunda7AssignmentParser());

    public UserTaskAssignment parse(UserTask task, AssignmentXml xml, List<String> activeProfiles) {
        List<Match> matches = new ArrayList<>();
        for (AssignmentDialectParser parser : parsers) {
            if (activeProfiles.contains(parser.profileId())) {
                try {
                    parser.parse(task, xml).ifPresent(value -> matches.add(new Match(parser.profileId(), value)));
                } catch (IllegalArgumentException exception) {
                    throw BpmnValidationException.single(new BpmnValidationIssue(
                            BpmnErrorCodes.INVALID_ASSIGNEE, ValidationSeverity.ERROR, exception.getMessage(),
                            null, task.getId(), null, null, "Correct the user-task assignment directive."));
                }
            }
        }
        if (matches.size() > 1) {
            throw BpmnValidationException.single(new BpmnValidationIssue(
                    BpmnErrorCodes.CONFLICTING_ASSIGNMENT, ValidationSeverity.ERROR,
                    "User task '" + task.getId() + "' defines assignment semantics through more than one BPMN dialect: "
                            + matches.stream().map(Match::profile).toList(),
                    null, task.getId(), null, null,
                    "Remove all but one assignment representation."));
        }
        return matches.isEmpty() ? UserTaskAssignment.EMPTY : matches.getFirst().assignment();
    }

    private record Match(String profile, UserTaskAssignment assignment) {}
}
