package com.abada.engine.core.model.assignment;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/** Vendor-neutral user-task assignment retained in a compiled definition. */
public record UserTaskAssignment(
        Optional<ProcessExpression> assignee,
        List<ProcessExpression> candidateUsers,
        List<ProcessExpression> candidateGroups,
        AssignmentStrategy strategy) implements Serializable {

    public static final UserTaskAssignment EMPTY =
            new UserTaskAssignment(Optional.empty(), List.of(), List.of(), AssignmentStrategy.CLAIM);

    public UserTaskAssignment {
        assignee = assignee == null ? Optional.empty() : assignee;
        candidateUsers = candidateUsers == null ? List.of() : List.copyOf(candidateUsers);
        candidateGroups = candidateGroups == null ? List.of() : List.copyOf(candidateGroups);
        strategy = strategy == null ? AssignmentStrategy.CLAIM : strategy;
    }
}
