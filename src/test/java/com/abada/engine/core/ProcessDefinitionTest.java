package com.abada.engine.core;

import com.abada.engine.parser.BpmnParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessDefinitionTest {

    private ProcessDefinition definition;

    @BeforeEach
    void setup() {
        Map<String, BpmnParser.TaskMeta> tasks = Map.of(
                "taskA", taskMeta("Review", "alice", List.of("bob"), List.of("team")),
                "taskB", taskMeta("Approve", null, List.of("carol"), List.of("qa"))
        );

        List<BpmnParser.SequenceFlow> flows = List.of(
                new BpmnParser.SequenceFlow("f1", "startEvent1", "taskA"),
                new BpmnParser.SequenceFlow("f2", "taskA", "taskB"),
                new BpmnParser.SequenceFlow("f3", "taskB", "endEvent1")
        );

        definition = new ProcessDefinition("demoProc", "Demo Process", "startEvent1", tasks, flows);
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
    void shouldReturnNextElementId() {
        assertEquals("taskA", definition.getNextElement("startEvent1"));
        assertEquals("taskB", definition.getNextElement("taskA"));
        assertEquals("endEvent1", definition.getNextElement("taskB"));
    }

    @Test
    void shouldRecognizeUserTasks() {
        assertTrue(definition.isUserTask("taskA"));
        assertTrue(definition.isUserTask("taskB"));
        assertFalse(definition.isUserTask("startEvent1"));
    }

    @Test
    void shouldReturnTaskMetadata() {
        assertEquals("Review", definition.getTaskName("taskA"));
        assertEquals("alice", definition.getTaskAssignee("taskA"));
        assertEquals(List.of("bob"), definition.getCandidateUsers("taskA"));
        assertEquals(List.of("team"), definition.getCandidateGroups("taskA"));

        assertNull(definition.getTaskAssignee("taskB"));
        assertEquals(List.of("carol"), definition.getCandidateUsers("taskB"));
        assertEquals(List.of("qa"), definition.getCandidateGroups("taskB"));
    }
}
