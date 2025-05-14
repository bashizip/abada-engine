package com.abada.engine.api;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.TaskInstance;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EngineIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private UserContextProvider context; // <-- inject a fake Context

    @BeforeEach
    void setupContext() {
        // Default user to "alice" before each test
        when(context.getUsername()).thenReturn("alice");
        when(context.getGroups()).thenReturn(List.of("customers"));
    }

    private void setupTestProcess() throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource file = new ByteArrayResource(
                BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn").readAllBytes()) {
            @Override
            public String getFilename() {
                return "recipe-cook.bpmn";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        restTemplate.postForEntity("/engine/deploy", request, String.class);

        HttpHeaders startHeaders = new HttpHeaders();
        startHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> startRequest = new HttpEntity<>("processId=recipe-cook", startHeaders);
        restTemplate.postForEntity("/engine/start", startRequest, String.class);

    }

    @Test
    void fullProcessExecution_shouldCompleteAllTasksAndFinishProcess() throws IOException {
        setupTestProcess();

        // 1. Alice sees the first task
        ResponseEntity<TaskInstance[]> taskResponse1 = restTemplate.getForEntity(
                "/engine/tasks", TaskInstance[].class);
        assertEquals(HttpStatus.OK, taskResponse1.getStatusCode());
        TaskInstance[] tasksForAlice = taskResponse1.getBody();
        assertNotNull(tasksForAlice);
        assertTrue(tasksForAlice.length > 0, "Alice should see a task");

        String taskId1 = tasksForAlice[0].getId();

        // 2. Alice claims and completes the task
        restTemplate.postForEntity("/engine/claim?taskId=" + taskId1, null, String.class);
        restTemplate.postForEntity("/engine/complete?taskId=" + taskId1, null, String.class);

        // 3. Switch context to Bob in 'cuistos' group
        when(context.getUsername()).thenReturn("bob");
        when(context.getGroups()).thenReturn(List.of("cuistos"));

        // 4. Bob sees the second task
        ResponseEntity<TaskInstance[]> taskResponse2 = restTemplate.getForEntity(
                "/engine/tasks", TaskInstance[].class);
        assertEquals(HttpStatus.OK, taskResponse2.getStatusCode());
        TaskInstance[] tasksForBob = taskResponse2.getBody();
        assertNotNull(tasksForBob);
        assertTrue(tasksForBob.length > 0, "Bob (manager) should see a second task");

        String taskId2 = tasksForBob[0].getId();

        // 5. Bob claims and completes the task
        restTemplate.postForEntity("/engine/claim?taskId=" + taskId2, null, String.class);
        restTemplate.postForEntity("/engine/complete?taskId=" + taskId2, null, String.class);

        // 6. No more tasks should remain
        ResponseEntity<TaskInstance[]> finalTasks = restTemplate.getForEntity(
                "/engine/tasks", TaskInstance[].class);
        TaskInstance[] remainingTasks = finalTasks.getBody();
        assertNotNull(remainingTasks);
        assertEquals(0, remainingTasks.length, "No tasks should remain, process should be completed");
    }
}
