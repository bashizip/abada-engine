package com.abada.engine.parser.assignment;

import com.abada.engine.bpmn.compatibility.BpmnErrorCodes;
import com.abada.engine.bpmn.compatibility.BpmnValidationException;
import com.abada.engine.core.model.assignment.*;
import com.abada.engine.parser.BpmnParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssignmentParserRegistryTest {
    @Test void parsesCompactAbadaAssignment() {
        var assignment = assignment("""
            <bpmn:extensionElements><abada:assignment assignee="${owner}" candidateUsers="alice,bob,alice"
              candidateGroups="finance" strategy="direct"/></bpmn:extensionElements>
            """, "");
        assertThat(assignment.assignee()).contains(new DynamicExpression("${owner}"));
        assertThat(assignment.candidateUsers()).containsExactly(new LiteralExpression("alice"), new LiteralExpression("bob"));
        assertThat(assignment.candidateGroups()).containsExactly(new LiteralExpression("finance"));
        assertThat(assignment.strategy()).isEqualTo(AssignmentStrategy.DIRECT);
    }

    @Test void parsesNestedAbadaAssignment() {
        var assignment = assignment("""
            <bpmn:extensionElements><abada:assignment strategy="claim">
              <abada:assignee expression="${owner}"/><abada:candidateUsers>
                <abada:user value="alice"/><abada:user expression="${reviewer}"/>
              </abada:candidateUsers><abada:candidateGroups><abada:group value="finance"/></abada:candidateGroups>
            </abada:assignment></bpmn:extensionElements>
            """, "");
        assertThat(assignment.assignee()).contains(new DynamicExpression("${owner}"));
        assertThat(assignment.candidateUsers()).containsExactly(new LiteralExpression("alice"), new DynamicExpression("${reviewer}"));
        assertThat(assignment.strategy()).isEqualTo(AssignmentStrategy.CLAIM);
    }

    @Test void parsesStandardPotentialOwnerAndHumanPerformer() {
        var assignment = assignment("""
            <bpmn:humanPerformer><bpmn:resourceAssignmentExpression><bpmn:formalExpression>user:alice</bpmn:formalExpression></bpmn:resourceAssignmentExpression></bpmn:humanPerformer>
            <bpmn:potentialOwner><bpmn:resourceAssignmentExpression><bpmn:formalExpression>group:finance</bpmn:formalExpression></bpmn:resourceAssignmentExpression></bpmn:potentialOwner>
            """, "");
        assertThat(assignment.assignee()).contains(new LiteralExpression("alice"));
        assertThat(assignment.candidateGroups()).containsExactly(new LiteralExpression("finance"));
    }

    @Test void rejectsMultipleDialectsWithStableCode() {
        assertThatThrownBy(() -> assignment("""
            <bpmn:extensionElements><abada:assignment assignee="bob"/></bpmn:extensionElements>
            """, "camunda:assignee=\"alice\""))
            .isInstanceOf(BpmnValidationException.class)
            .satisfies(error -> assertThat(((BpmnValidationException) error).getIssues().getFirst().code())
                    .isEqualTo(BpmnErrorCodes.CONFLICTING_ASSIGNMENT));
    }

    private UserTaskAssignment assignment(String body, String attributes) {
        String xml = """
            <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
              xmlns:abada="https://abada.io/schema/bpmn" xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
              targetNamespace="test"><bpmn:process id="p"><bpmn:startEvent id="s"/>
              <bpmn:userTask id="t" %s>%s</bpmn:userTask><bpmn:endEvent id="e"/>
              <bpmn:sequenceFlow id="a" sourceRef="s" targetRef="t"/><bpmn:sequenceFlow id="b" sourceRef="t" targetRef="e"/>
              </bpmn:process></bpmn:definitions>
            """.formatted(attributes, body);
        return new BpmnParser().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                .getUserTask("t").getAssignment();
    }
}
