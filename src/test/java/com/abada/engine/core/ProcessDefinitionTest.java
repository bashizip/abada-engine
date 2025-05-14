package com.abada.engine.core;

import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.SequenceFlow;
import com.abada.engine.core.model.TaskMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ProcessDefinitionTest {

    private ParsedProcessDefinition definition;

    @BeforeEach
    void setup() {
        Map<String,TaskMeta> tasks = Map.of(
                "taskA", taskMeta("Review", "alice", List.of("bob"), List.of("team")),
                "taskB", taskMeta("Approve", null, List.of("carol"), List.of("qa"))
        );

        List<SequenceFlow> flows = List.of(
                new SequenceFlow("f1", "startEvent1", "taskA"),
                new SequenceFlow("f2", "taskA", "taskB"),
                new SequenceFlow("f3", "taskB", "endEvent1")
        );

        definition = new ParsedProcessDefinition("demoProc", "Demo Process", "startEvent1", tasks, flows, "<bpmn>...</bpmn>");
    }

    private TaskMeta taskMeta(String name, String assignee, List<String> users, List<String> groups) {
        TaskMeta meta = new TaskMeta();
        meta.name = name;
        meta.assignee = assignee;
        meta.candidateUsers = users;
        meta.candidateGroups = groups;
        return meta;
    }

    @Test
    void shouldReturnNextElementId() {
        assertEquals("taskA", definition.getNextActivity("startEvent1"));
        assertEquals("taskB", definition.getNextActivity("taskA"));
        assertEquals("endEvent1", definition.getNextActivity("taskB"));
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
