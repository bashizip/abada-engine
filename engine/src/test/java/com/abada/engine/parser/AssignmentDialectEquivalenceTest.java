package com.abada.engine.parser;

import com.abada.engine.core.model.assignment.LiteralExpression;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AssignmentDialectEquivalenceTest {
    @Test void runnableStandardNativeAndCamundaExamplesCompileToEquivalentGroupAssignment() throws Exception {
        BpmnParser parser = new BpmnParser();
        for (String fixture : new String[]{"standard-assignment.bpmn", "abada-native-assignment.bpmn",
                "camunda-7-assignment.bpmn"}) {
            try (var input = Files.newInputStream(Path.of("../examples/bpmn", fixture))) {
                assertThat(parser.parse(input).getUserTask("approve").getAssignment().candidateGroups())
                        .containsExactly(new LiteralExpression("finance"));
            }
        }
    }
}
