package com.abada.engine.core;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class ParallelGatewayTest {

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private TaskManager taskManager;

    @Mock
    private UserContextProvider context;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));

        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("parallel-gateway-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    @Test
    @DisplayName("Parallel gateway should fork, wait for all paths to join, and complete")
    void shouldForkJoinAndCompleteCorrectly() {
        // 1. Start the process
        ProcessInstance pi = abadaEngine.startProcess("ParallelGatewayProcess");

        // 2. Complete the initial setup task
        List<TaskInstance> initialTasks = taskManager.getTasksForProcessInstance(pi.getId());
        assertEquals(1, initialTasks.size());
        TaskInstance initialTask = initialTasks.get(0);
        assertEquals("InitialTask", initialTask.getTaskDefinitionKey());
        abadaEngine.completeTask(initialTask.getId(), "test-user", List.of(), Map.of());

        // 3. Assert that the process has forked into two parallel tasks
        List<TaskInstance> forkedTasks = taskManager.getTasksForProcessInstance(pi.getId());
        assertEquals(2, forkedTasks.size(), "Should have forked into two tasks");
        assertTrue(forkedTasks.stream().anyMatch(t -> t.getTaskDefinitionKey().equals("TaskA")));
        assertTrue(forkedTasks.stream().anyMatch(t -> t.getTaskDefinitionKey().equals("TaskB")));

        // 4. Complete the first parallel task (Task A)
        TaskInstance taskA = forkedTasks.stream().filter(t -> t.getTaskDefinitionKey().equals("TaskA")).findFirst().orElseThrow();
        abadaEngine.completeTask(taskA.getId(), "test-user", List.of(), Map.of());

        // 5. Assert that the process is still active and waiting for the other path
        ProcessInstance piAfterOneCompletion = abadaEngine.getProcessInstanceById(pi.getId());
        assertFalse(piAfterOneCompletion.isCompleted(), "Process should not be complete yet");
        List<TaskInstance> remainingTasks = taskManager.getTasksForProcessInstance(pi.getId());
        assertEquals(1, remainingTasks.size(), "Should be waiting for one more task");
        assertEquals("TaskB", remainingTasks.get(0).getTaskDefinitionKey(), "Task B should be the only one remaining");

        // 6. Complete the second parallel task (Task B)
        TaskInstance taskB = remainingTasks.get(0);
        abadaEngine.completeTask(taskB.getId(), "test-user", List.of(), Map.of());

        // 7. Assert that the process has joined and completed successfully
        ProcessInstance finalPi = abadaEngine.getProcessInstanceById(pi.getId());
        assertTrue(finalPi.isCompleted(), "Process should be completed after the join");
        assertTrue(taskManager.getTasksForProcessInstance(pi.getId()).isEmpty(), "There should be no active tasks left");
    }
}
