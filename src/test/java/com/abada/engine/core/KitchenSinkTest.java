package com.abada.engine.core;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.context.UserContextProvider;
import com.abada.engine.dto.FetchAndLockRequest;
import com.abada.engine.dto.LockedExternalTask;
import com.abada.engine.persistence.repository.ExternalTaskRepository;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
public class KitchenSinkTest {

    @Autowired

    private AbadaEngine abadaEngine;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private EventManager eventManager;

    @Autowired
    private ExternalTaskRepository externalTaskRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private UserContextProvider context;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        externalTaskRepository.deleteAll();
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));

        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("kitchen-sink-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    @Test
    @DisplayName("Should execute a complex process with all gateway and event features")
    void shouldExecuteComplexProcess() {
        // 1. Start the process and complete the initial task to set variables
        ProcessInstance pi = abadaEngine.startProcess("KitchenSinkProcess");
        String correlationKey = UUID.randomUUID().toString();
        abadaEngine.completeTask(
                taskManager.getTasksForProcessInstance(pi.getId()).get(0).getId(),
                "test-user",
                List.of(),
                Map.of("correlationKey", correlationKey, "path", "C")
        );

        // 2. Assert that the parallel fork worked.
        // The embedded service task on Path A should execute instantly.
        // The process should now be waiting only on Path B at the message event.
        ProcessInstance piAfterFork = abadaEngine.getProcessInstanceById(pi.getId());
        assertEquals(true, piAfterFork.getVariable("delegateExecuted"), "Embedded delegate should have executed");
        assertEquals(1, piAfterFork.getActiveTokens().size(), "Should only be waiting on one path");
        assertEquals("MessageCatch", piAfterFork.getActiveTokens().get(0), "Should be waiting for the message");

        // 3. Trigger the message event to advance the second parallel path
        eventManager.correlateMessage("FastTrackMessage", correlationKey, Map.of());

        // 4. Assert that the parallel join has occurred and the inclusive gateway has routed to Task C
        ProcessInstance piAfterJoin = abadaEngine.getProcessInstanceById(pi.getId());
        assertEquals(1, piAfterJoin.getActiveTokens().size());
        assertEquals("TaskC", piAfterJoin.getActiveTokens().get(0));

        // 5. Complete Task C. This will pass through the inclusive join and hit the external task.
        abadaEngine.completeTask(taskManager.getTasksForProcessInstance(pi.getId()).get(0).getId(), "test-user", List.of(), Map.of());
        assertEquals(1, externalTaskRepository.count(), "An external task job should have been created");

        // 6. Simulate an external worker fetching and completing the job via the API
        FetchAndLockRequest fetchRequest = new FetchAndLockRequest("worker-kitchen-sink", List.of("kitchen-sink-topic"), 10000L);
        ResponseEntity<List<LockedExternalTask>> lockResponse = restTemplate.exchange(
                "/api/v1/external-tasks/fetch-and-lock", HttpMethod.POST, new org.springframework.http.HttpEntity<>(fetchRequest), new ParameterizedTypeReference<>() {}
        );
        assertEquals(HttpStatus.OK, lockResponse.getStatusCode());
        LockedExternalTask lockedTask = lockResponse.getBody().get(0);

        restTemplate.postForEntity("/api/v1/external-tasks/{id}/complete", Map.of("externalTaskDone", true), Void.class, lockedTask.id());

        // 7. Assert that the process is now complete
        ProcessInstance finalPi = abadaEngine.getProcessInstanceById(pi.getId());
        assertTrue(finalPi.isCompleted(), "Process should be completed");
        assertEquals(true, finalPi.getVariable("externalTaskDone"));
        assertEquals(0, externalTaskRepository.count(), "External task should be deleted");
    }
}
