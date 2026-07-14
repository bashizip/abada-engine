package com.abada.engine.bpmn.migration;

import com.abada.engine.core.model.assignment.DynamicExpression;
import com.abada.engine.core.model.assignment.LiteralExpression;
import com.abada.engine.parser.BpmnParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BpmnMigrationServiceTest {
    @Test void migratesCamundaAssignmentAndPreservesCanonicalMeaningAndOriginalInput() {
        String original = xml("camunda:assignee=\"${owner}\" camunda:candidateUsers=\"alice,bob\" camunda:candidateGroups=\"finance\"");
        var result = new BpmnMigrationService().migrate(stream(original));

        assertThat(result.originalXml()).isEqualTo(original);
        assertThat(result.migratedXml()).contains("abada:assignment").doesNotContain("camunda:assignee");
        var assignment = new BpmnParser().parse(stream(result.migratedXml())).getUserTask("t").getAssignment();
        assertThat(assignment.assignee()).contains(new DynamicExpression("${owner}"));
        assertThat(assignment.candidateUsers()).containsExactly(new LiteralExpression("alice"), new LiteralExpression("bob"));
        assertThat(assignment.candidateGroups()).containsExactly(new LiteralExpression("finance"));
        assertThat(result.report().mappings()).hasSize(1);
    }

    private String xml(String attributes) {
        return """
            <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
              xmlns:camunda="http://camunda.org/schema/1.0/bpmn" targetNamespace="test">
              <bpmn:process id="p"><bpmn:startEvent id="s"/><bpmn:userTask id="t" %s/><bpmn:endEvent id="e"/>
              <bpmn:sequenceFlow id="a" sourceRef="s" targetRef="t"/><bpmn:sequenceFlow id="b" sourceRef="t" targetRef="e"/>
              </bpmn:process></bpmn:definitions>
            """.formatted(attributes);
    }
    private ByteArrayInputStream stream(String value) { return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)); }
}
