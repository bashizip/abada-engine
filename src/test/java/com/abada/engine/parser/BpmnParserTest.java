package com.abada.engine.parser;

import com.abada.engine.core.ParsedProcessDefinition;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BpmnParserTest {


    @Test
    void shouldParseBasicProcessCorrectly() {
        InputStream inputStream = getClass().getResourceAsStream("/bpmn/test-process.bpmn");
        assertNotNull(inputStream, "Could not find BPMN test file!");

        BpmnParser parser = new BpmnParser();
        ParsedProcessDefinition definition = parser.parse(inputStream);

        assertEquals("test-process", definition.getId());
        assertTrue(definition.isUserTask("task1"));
        assertEquals("Do something", definition.getTaskName("task1"));
        assertEquals("bob", definition.getTaskAssignee("task1"));
        assertEquals(List.of("alice"), definition.getCandidateUsers("task1"));
        assertEquals(List.of("finance", "qa"), definition.getCandidateGroups("task1"));
    }
}
