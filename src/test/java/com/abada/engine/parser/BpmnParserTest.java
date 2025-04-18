package com.abada.engine.parser;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BpmnParserTest {

    private static final String SAMPLE_BPMN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
            "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "             xsi:schemaLocation=\"http://www.omg.org/spec/BPMN/20100524/MODEL BPMN20.xsd\"\n" +
            "             typeLanguage=\"http://www.w3.org/2001/XMLSchema\"\n" +
            "             expressionLanguage=\"http://www.w3.org/1999/XPath\"\n" +
            "             targetNamespace=\"http://abada.engine/test\">\n" +
            "  <process id=\"test-process\" name=\"Test Process\" isExecutable=\"true\">\n" +
            "    <startEvent id=\"startEvent1\" name=\"Start\" />\n" +
            "    <sequenceFlow id=\"flow1\" sourceRef=\"startEvent1\" targetRef=\"userTask1\" />\n" +
            "    <userTask id=\"userTask1\" name=\"Review\" assignee=\"john\" candidateUsers=\"alice,bob\" candidateGroups=\"managers,hr\" />\n" +
            "    <sequenceFlow id=\"flow2\" sourceRef=\"userTask1\" targetRef=\"endEvent1\" />\n" +
            "    <endEvent id=\"endEvent1\" name=\"End\" />\n" +
            "  </process>\n" +
            "</definitions>\n";

    @Test
    public void shouldParseBasicProcessCorrectly() throws Exception {
        BpmnParser parser = new BpmnParser();
        BpmnParser.ParsedProcess process = parser.parse(new ByteArrayInputStream(SAMPLE_BPMN.getBytes(StandardCharsets.UTF_8)));

        assertEquals("test-process", process.id);
        assertEquals("Test Process", process.name);
        assertEquals("startEvent1", process.startEventId);
        assertEquals(1, process.userTasks.size());

        BpmnParser.TaskMeta task = process.userTasks.get("userTask1");
        assertNotNull(task);
        assertEquals("Review", task.name);
        assertEquals("john", task.assignee);
        assertIterableEquals(List.of("alice", "bob"), task.candidateUsers);
        assertIterableEquals(List.of("managers", "hr"), task.candidateGroups);

        assertEquals(2, process.sequenceFlows.size());
    }
}
