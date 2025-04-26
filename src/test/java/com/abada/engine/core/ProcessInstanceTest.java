package com.abada.engine.core;

import com.abada.engine.parser.BpmnParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessInstanceTest {

    private ParsedProcessDefinition definition;

    @BeforeEach
    void setup() {
        Map<String, BpmnParser.TaskMeta> tasks = Map.of(
                "task1", taskMeta("Review", "alice", List.of(), List.of())
        );

        List<BpmnParser.SequenceFlow> flows = List.of(
                new BpmnParser.SequenceFlow("s1", "startEvent1", "task1"),
                new BpmnParser.SequenceFlow("s2", "task1", "endEvent1")
        );

        definition = new ParsedProcessDefinition("proc", "Simple Process", "startEvent1", tasks, flows, "<bpmn>...</bpmn>");
    }

    private BpmnParser.TaskMeta taskMeta(String name, String assignee, List<String> users, List<String> groups) {
        BpmnParser.TaskMeta meta = new BpmnParser.TaskMeta();
        meta.name = name;
        meta.assignee = assignee;
        meta.candidateUsers = users;
        meta.candidateGroups = groups;
        return meta;
    }

    @Test
    void shouldAdvanceThroughProcess() {
        ProcessInstance instance = new ProcessInstance(definition);

        // After construction, currentElement should be the startEvent
        assertEquals("startEvent1", instance.getCurrentElementId());
        assertFalse(instance.isUserTask());

        // Advance to task1
        String step1 = instance.advance();
        assertEquals("task1", step1);
        assertEquals("task1", instance.getCurrentElementId());
        assertTrue(instance.isUserTask());

        // Advance to endEvent1
        String step2 = instance.advance();
        assertEquals("endEvent1", step2);
        assertEquals("endEvent1", instance.getCurrentElementId());
        assertFalse(instance.isUserTask());

        // No more steps
        assertNull(instance.advance());
    }
}
