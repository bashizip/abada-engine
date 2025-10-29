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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class MessageEventTest {

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private EventManager eventManager;

    @Mock
    private UserContextProvider context;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));

        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("message-event-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    @Test
    @DisplayName("Process should wait for and correctly receive a message event")
    void shouldWaitForAndReceiveMessageEvent() {
        // 1. Start the process
        ProcessInstance pi = abadaEngine.startProcess("MessageEventProcess");
        String correlationKey = UUID.randomUUID().toString();

        // 2. Complete the initial task to set the correlation key
        TaskInstance initialTask = taskManager.getTasksForProcessInstance(pi.getId()).get(0);
        assertEquals("Task_SetKey", initialTask.getTaskDefinitionKey());
        abadaEngine.completeTask(initialTask.getId(), "test-user", List.of(), Map.of("correlationKey", correlationKey));

        // 3. Assert that the process is now waiting at the message catch event
        ProcessInstance waitingPi = abadaEngine.getProcessInstanceById(pi.getId());
        assertFalse(waitingPi.isCompleted(), "Process should be in a wait state");
        assertEquals(1, waitingPi.getActiveTokens().size(), "Should be one active token");
        assertEquals("CatchEvent_OrderPaid", waitingPi.getActiveTokens().get(0), "Should be waiting at the message event");
        assertTrue(taskManager.getTasksForProcessInstance(pi.getId()).isEmpty(), "There should be no user tasks");

        // 4. Correlate the message to resume the process
        eventManager.correlateMessage("OrderPaid", correlationKey, Map.of("paymentStatus", "PAID"));

        // 5. Assert that the process has moved to the next task
        ProcessInstance resumedPi = abadaEngine.getProcessInstanceById(pi.getId());
        List<TaskInstance> finalTasks = taskManager.getTasksForProcessInstance(resumedPi.getId());
        assertEquals(1, finalTasks.size(), "Should have moved to the final task");
        assertEquals("Task_FulfillOrder", finalTasks.get(0).getTaskDefinitionKey());
        assertEquals("PAID", resumedPi.getVariable("paymentStatus"), "Variables from message should be merged");

        // 6. Complete the final task and assert the process is finished
        abadaEngine.completeTask(finalTasks.get(0).getId(), "test-user", List.of(), Map.of());
        assertTrue(abadaEngine.getProcessInstanceById(pi.getId()).isCompleted(), "Process should be completed");
    }
}
