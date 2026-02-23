package com.abada.engine.core;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
public class SignalEventTest {

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private EventManager eventManager;

    @MockBean
    private UserContextProvider context;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));

        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("signal-event-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    @Test
    @DisplayName("Signal event should be broadcast to all waiting instances")
    void shouldBroadcastSignalToMultipleInstances() {
        // 1. Start two separate process instances
        ProcessInstance pi1 = abadaEngine.startProcess("SignalEventProcess");
        ProcessInstance pi2 = abadaEngine.startProcess("SignalEventProcess");

        // 2. Complete the initial task for both to move them to the signal wait state
        abadaEngine.completeTask(taskManager.getTasksForProcessInstance(pi1.getId()).get(0).getId(), "test-user", List.of(), Map.of());
        abadaEngine.completeTask(taskManager.getTasksForProcessInstance(pi2.getId()).get(0).getId(), "test-user", List.of(), Map.of());

        // 3. Assert that both instances are waiting at the signal event
        assertEquals("CatchEvent_Signal", abadaEngine.getProcessInstanceById(pi1.getId()).getActiveTokens().get(0));
        assertEquals("CatchEvent_Signal", abadaEngine.getProcessInstanceById(pi2.getId()).getActiveTokens().get(0));
        assertEquals(0, taskManager.getTasksForProcessInstance(pi1.getId()).size());
        assertEquals(0, taskManager.getTasksForProcessInstance(pi2.getId()).size());

        // 4. Broadcast the signal
        eventManager.broadcastSignal("SignalGo", Map.of("signalData", "ImportantInfo"));

        // 5. Assert that both instances have been resumed and are at the final task
        List<TaskInstance> finalTasks1 = taskManager.getTasksForProcessInstance(pi1.getId());
        List<TaskInstance> finalTasks2 = taskManager.getTasksForProcessInstance(pi2.getId());

        assertEquals(1, finalTasks1.size(), "Instance 1 should have moved to the final task");
        assertEquals("FinalTask", finalTasks1.get(0).getTaskDefinitionKey());
        assertEquals("ImportantInfo", abadaEngine.getProcessInstanceById(pi1.getId()).getVariable("signalData"));

        assertEquals(1, finalTasks2.size(), "Instance 2 should have moved to the final task");
        assertEquals("FinalTask", finalTasks2.get(0).getTaskDefinitionKey());
        assertEquals("ImportantInfo", abadaEngine.getProcessInstanceById(pi2.getId()).getVariable("signalData"));

        // 6. Complete the final tasks and assert both processes are finished
        abadaEngine.completeTask(finalTasks1.get(0).getId(), "test-user", List.of(), Map.of());
        abadaEngine.completeTask(finalTasks2.get(0).getId(), "test-user", List.of(), Map.of());

        assertTrue(abadaEngine.getProcessInstanceById(pi1.getId()).isCompleted());
        assertTrue(abadaEngine.getProcessInstanceById(pi2.getId()).isCompleted());
    }
}
