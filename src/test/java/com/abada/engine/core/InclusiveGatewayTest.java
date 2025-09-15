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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class InclusiveGatewayTest {

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private TaskManager taskManager;

    @MockBean
    private UserContextProvider context;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));

        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("order-fulfillment.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    private TaskInstance getAndCompleteInitialTask(ProcessInstance pi, Map<String, Object> variables) {
        List<TaskInstance> initialTasks = taskManager.getTasksForProcessInstance(pi.getId());
        assertEquals(1, initialTasks.size());
        TaskInstance initialTask = initialTasks.get(0);
        assertEquals("Task_SetDetails", initialTask.getTaskDefinitionKey());

        boolean completed = abadaEngine.completeTask(initialTask.getId(), "test-user", List.of(), variables);
        assertTrue(completed);
        return initialTask;
    }

    @Test
    @DisplayName("Inclusive gateway should fork into multiple paths when conditions are met")
    void shouldFollowMultiplePathsWhenConditionsAreTrue() {
        // Given
        ProcessInstance pi = abadaEngine.startProcess("OrderFulfillmentProcess");
        Map<String, Object> variables = Map.of("order", Map.of(
                "hasPhysicalItems", true,
                "hasDigitalItems", true,
                "hasDiscountCode", false
        ));

        // When
        getAndCompleteInitialTask(pi, variables);

        // Then
        List<TaskInstance> tasks = taskManager.getTasksForProcessInstance(pi.getId());
        assertEquals(2, tasks.size(), "Should have created two user tasks");

        List<String> taskDefinitionKeys = tasks.stream().map(TaskInstance::getTaskDefinitionKey).collect(Collectors.toList());
        assertTrue(taskDefinitionKeys.contains("Activity_Physical"));
        assertTrue(taskDefinitionKeys.contains("Activity_Digital"));
    }

    @Test
    @DisplayName("Inclusive gateway should follow a single path when only one condition is true")
    void shouldFollowSinglePathWhenOnlyOneConditionIsTrue() {
        // Given
        ProcessInstance pi = abadaEngine.startProcess("OrderFulfillmentProcess");
        Map<String, Object> variables = Map.of("order", Map.of(
                "hasPhysicalItems", false,
                "hasDigitalItems", false,
                "hasDiscountCode", true
        ));

        // When
        getAndCompleteInitialTask(pi, variables);

        // Then
        List<TaskInstance> tasks = taskManager.getTasksForProcessInstance(pi.getId());
        assertEquals(1, tasks.size(), "Should have created one user task");
        assertEquals("Activity_Discount", tasks.get(0).getTaskDefinitionKey());
    }

    @Test
    @DisplayName("Inclusive gateway should join and complete when all forked paths are done")
    void shouldJoinAndCompleteWhenAllPathsAreDone() {
        // Given: A process with two active paths
        ProcessInstance pi = abadaEngine.startProcess("OrderFulfillmentProcess");
        Map<String, Object> variables = Map.of("order", Map.of(
                "hasPhysicalItems", true,
                "hasDigitalItems", true,
                "hasDiscountCode", false
        ));
        getAndCompleteInitialTask(pi, variables);

        List<TaskInstance> forkedTasks = taskManager.getTasksForProcessInstance(pi.getId());
        assertEquals(2, forkedTasks.size(), "Should be waiting at two user tasks");

        // When: First task is completed
        TaskInstance physicalTask = forkedTasks.stream().filter(t -> t.getTaskDefinitionKey().equals("Activity_Physical")).findFirst().orElseThrow();
        abadaEngine.completeTask(physicalTask.getId(), "test-user", List.of(), Map.of());

        // Then: The process should wait at the join gateway
        ProcessInstance piAfterFirstCompletion = abadaEngine.getProcessInstanceById(pi.getId());
        assertEquals(1, piAfterFirstCompletion.getActiveTokens().size(), "Should still have one active task");
        assertEquals("Activity_Digital", piAfterFirstCompletion.getActiveTokens().get(0));
        assertFalse(piAfterFirstCompletion.isCompleted(), "Process should not be completed yet");

        // When: Second task is completed
        TaskInstance digitalTask = taskManager.getTasksForProcessInstance(pi.getId()).stream().filter(t -> t.getTaskDefinitionKey().equals("Activity_Digital")).findFirst().orElseThrow();
        abadaEngine.completeTask(digitalTask.getId(), "test-user", List.of(), Map.of());

        // Then: The process should proceed from the join and complete
        ProcessInstance finalPi = abadaEngine.getProcessInstanceById(pi.getId());
        assertTrue(finalPi.isCompleted(), "Process should be completed");
    }
}
