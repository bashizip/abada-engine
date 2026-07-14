package com.abada.engine.parser.assignment;

import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.assignment.AssignmentStrategy;
import com.abada.engine.core.model.assignment.DynamicExpression;
import com.abada.engine.core.model.assignment.LiteralExpression;
import com.abada.engine.parser.BpmnParser;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Camunda7AssignmentParserTest {
    @Test void mapsAndNormalizesSupportedCamundaAssignment() {
        String xml = """
                <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                  xmlns:camunda="http://camunda.org/schema/1.0/bpmn" targetNamespace="https://abada.io/test">
                  <bpmn:process id="p"><bpmn:startEvent id="s"/><bpmn:userTask id="t"
                    camunda:assignee="${owner}" camunda:candidateUsers=" alice, bob,alice,, "
                    camunda:candidateGroups=" finance, finance "/><bpmn:endEvent id="e"/>
                    <bpmn:sequenceFlow id="a" sourceRef="s" targetRef="t"/>
                    <bpmn:sequenceFlow id="b" sourceRef="t" targetRef="e"/>
                  </bpmn:process>
                </bpmn:definitions>
                """;
        ParsedProcessDefinition definition = new BpmnParser().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        var assignment = definition.getUserTask("t").getAssignment();
        assertThat(assignment.assignee()).contains(new DynamicExpression("${owner}"));
        assertThat(assignment.candidateUsers()).containsExactly(
                new LiteralExpression("alice"), new LiteralExpression("bob"));
        assertThat(assignment.candidateGroups()).containsExactly(new LiteralExpression("finance"));
        assertThat(assignment.strategy()).isEqualTo(AssignmentStrategy.DIRECT);
    }
}
