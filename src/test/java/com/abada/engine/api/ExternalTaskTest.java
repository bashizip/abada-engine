package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
public class ExternalTaskTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private ExternalTaskRepository externalTaskRepository;

    @MockBean
    private UserContextProvider context;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        externalTaskRepository.deleteAll();
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));

        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("external-task-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    @Test
    @DisplayName("External worker should fetch, lock, and complete an external task")
    void shouldCreateAndCompleteExternalTaskViaApi() {
        // 1. Start the process
        ProcessInstance pi = abadaEngine.startProcess("ExternalTaskTestProcess");

        // 2. Assert that the process is waiting at the service task and a job was created
        assertEquals("ServiceTask_External", pi.getActiveTokens().get(0));
        assertEquals(1, externalTaskRepository.count());

        // 3. Simulate a worker fetching and locking the task
        FetchAndLockRequest fetchRequest = new FetchAndLockRequest("worker-1", List.of("test-topic"), 10000L);
        ResponseEntity<List<LockedExternalTask>> lockResponse = restTemplate.exchange(
                "/api/v1/external-tasks/fetch-and-lock",
                HttpMethod.POST,
                new org.springframework.http.HttpEntity<>(fetchRequest),
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, lockResponse.getStatusCode());
        List<LockedExternalTask> lockedTasks = lockResponse.getBody();
        assertNotNull(lockedTasks);
        assertEquals(1, lockedTasks.size());
        LockedExternalTask lockedTask = lockedTasks.get(0);
        assertEquals("test-topic", lockedTask.topicName());

        // 4. Simulate the worker completing the task
        Map<String, Object> outputVariables = Map.of("externalTaskResult", "SUCCESS");
        restTemplate.postForEntity("/api/v1/external-tasks/{id}/complete", outputVariables, Void.class, lockedTask.id());

        // 5. Assert that the process has resumed and moved to the final user task
        ProcessInstance resumedPi = abadaEngine.getProcessInstanceById(pi.getId());
        assertEquals("FinalTask", resumedPi.getActiveTokens().get(0));
        assertEquals("SUCCESS", resumedPi.getVariable("externalTaskResult"));

        // 6. Assert that the external task job has been deleted
        assertEquals(0, externalTaskRepository.count());
    }
}
