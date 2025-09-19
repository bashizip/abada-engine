package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.dto.TaskDetailsDto;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TaskControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AbadaEngine abadaEngine;

    @MockBean
    private UserContextProvider context;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));

        // Deploy a process that starts with a user task
        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("service-task-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    @Test
    @DisplayName("GET /api/v1/tasks/{id} should return task details and process variables")
    void shouldReturnTaskDetailsById() {
        // 1. Start a process instance
        ProcessInstance pi = abadaEngine.startProcess("ServiceTaskTestProcess");
        pi.setVariable("initialVar", "testValue");

        // The process should be waiting at the first user task
        List<TaskInstance> tasks = abadaEngine.getTaskManager().getTasksForProcessInstance(pi.getId());
        assertFalse(tasks.isEmpty());
        String taskId = tasks.get(0).getId();

        // 2. Call the new endpoint
        ResponseEntity<TaskDetailsDto> response = restTemplate.getForEntity("/api/v1/tasks/{id}", TaskDetailsDto.class, taskId);

        // 3. Assert the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        TaskDetailsDto body = response.getBody();
        assertNotNull(body);

        assertEquals(taskId, body.id());
        assertEquals("UserTask_1", body.taskDefinitionKey());
        assertNotNull(body.variables());
        assertEquals("testValue", body.variables().get("initialVar"));
    }
}
