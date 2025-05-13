package com.abada.engine.core;

import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.dto.UserTaskPayload;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessInstanceTest {

    @Test
    void testAdvanceThroughSimpleTwoTaskProcess() {
        ParsedProcessDefinition def = BpmnTestUtils.parse("simple-two-task.bpmn");
        ProcessInstance instance = new ProcessInstance(def);

        // Step 1: Advance to userTask1
        Optional<UserTaskPayload> task1 = instance.advance();
        assertTrue(task1.isPresent(), "Expected first task");
        assertEquals("userTask1", task1.get().taskDefinitionKey());
        assertEquals("First Task", task1.get().name());
        assertEquals(List.of("alice"), task1.get().candidateUsers());
        assertTrue(task1.get().candidateGroups().isEmpty());

        // Step 2: Advance to userTask2
        Optional<UserTaskPayload> task2 = instance.advance();
        assertTrue(task2.isPresent(), "Expected second task");
        assertEquals("userTask2", task2.get().taskDefinitionKey());
        assertEquals("Second Task", task2.get().name());
        assertTrue(task2.get().candidateUsers().isEmpty());
        assertEquals(List.of("managers"), task2.get().candidateGroups());

        // Step 3: Advance to end
        Optional<UserTaskPayload> end = instance.advance();
        assertTrue(end.isEmpty(), "No more tasks expected");
        assertTrue(instance.isCompleted(), "Process should be completed");
    }
}
