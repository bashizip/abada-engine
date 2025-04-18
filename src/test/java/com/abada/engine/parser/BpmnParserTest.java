package com.abada.engine.parser;

import com.abada.engine.core.ProcessDefinition;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BpmnParserTest {

    private static final String SIMPLE_BPMN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
            "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "             targetNamespace=\"http://abada/engine/test\">\n" +
            "  <process id=\"test-process\" name=\"Test Process\" isExecutable=\"true\">\n" +
            "    <startEvent id=\"start\"/>\n" +
            "    <sequenceFlow id=\"flow1\" sourceRef=\"start\" targetRef=\"task1\"/>\n" +
            "    <userTask id=\"task1\" name=\"Do something\" assignee=\"bob\" candidateUsers=\"alice\" candidateGroups=\"finance,qa\"/>\n" +
            "    <sequenceFlow id=\"flow2\" sourceRef=\"task1\" targetRef=\"end\"/>\n" +
            "    <endEvent id=\"end\"/>\n" +
            "  </process>\n" +
            "</definitions>";

    @Test
    void shouldParseBasicProcessCorrectly() {
        BpmnParser parser = new BpmnParser();
        ByteArrayInputStream input = new ByteArrayInputStream(SIMPLE_BPMN.getBytes(StandardCharsets.UTF_8));

        ProcessDefinition definition = parser.parse(input);

        assertEquals("test-process", definition.getId());
        assertTrue(definition.isUserTask("task1"));
        assertEquals("Do something", definition.getTaskName("task1"));
        assertEquals("bob", definition.getTaskAssignee("task1"));
        assertEquals(List.of("alice"), definition.getCandidateUsers("task1"));
        assertEquals(List.of("finance", "qa"), definition.getCandidateGroups("task1"));
    }
}
