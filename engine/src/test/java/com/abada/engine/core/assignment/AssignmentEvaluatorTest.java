package com.abada.engine.core.assignment;

import com.abada.engine.core.exception.ProcessEngineException;
import com.abada.engine.core.model.assignment.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignmentEvaluatorTest {
    private final AssignmentEvaluator evaluator = new AssignmentEvaluator();

    @Test void resolvesNestedVariablesAndNormalizesCandidatesOnce() {
        var assignment = new UserTaskAssignment(Optional.of(new DynamicExpression("${request.owner}")),
                List.of(new DynamicExpression("${reviewers}"), new LiteralExpression(" alice ")),
                List.of(new DynamicExpression("${groups}")), AssignmentStrategy.DIRECT);

        var result = evaluator.evaluate(assignment, Map.of(
                "request", Map.of("owner", " bob "),
                "reviewers", List.of("alice", "", "carol", "alice"),
                "groups", List.of("finance", "finance")));

        assertThat(result.assignee()).isEqualTo("bob");
        assertThat(result.candidateUsers()).containsExactly("alice", "carol");
        assertThat(result.candidateGroups()).containsExactly("finance");
    }

    @Test void rejectsMultipleResolvedAssignees() {
        var assignment = new UserTaskAssignment(Optional.of(new DynamicExpression("${owners}")),
                List.of(), List.of(), AssignmentStrategy.DIRECT);
        assertThatThrownBy(() -> evaluator.evaluate(assignment, Map.of("owners", List.of("alice", "bob"))))
                .isInstanceOf(ProcessEngineException.class);
    }
}
