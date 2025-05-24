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
import org.springframework.core.ParameterizedTypeReference;
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
    private UserContextProvider context;

    @BeforeEach
    void setup() throws IOException {
        when(context.getUsername()).thenReturn("alice");
        when(context.getGroups()).thenReturn(List.of("customers"));
        deployAndStartProcess();
    }

    private void deployAndStartProcess() throws IOException {
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
        restTemplate.postForEntity("/v1/processes/deploy", request, String.class);

        HttpHeaders startHeaders = new HttpHeaders();
        startHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> startRequest = new HttpEntity<>("processId=recipe-cook", startHeaders);
        restTemplate.postForEntity("/v1/processes/start", startRequest, String.class);
    }

    @Test
    void shouldCompleteAllTasksAndFinishProcess() {
        ResponseEntity<String> rawResponse1 = restTemplate.getForEntity("/v1/tasks", String.class);
        System.out.println("Raw JSON for Alice: " + rawResponse1.getBody());
        assertEquals(HttpStatus.OK, rawResponse1.getStatusCode());

        ResponseEntity<List<TaskInstance>> taskResponse1 = restTemplate.exchange(
                "/v1/tasks",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        List<TaskInstance> tasksForAlice = taskResponse1.getBody();
        assertNotNull(tasksForAlice);
        assertFalse(tasksForAlice.isEmpty());

        String taskId1 = tasksForAlice.get(0).getId();
        restTemplate.postForEntity("/v1/tasks/claim?taskId=" + taskId1, null, String.class);
        restTemplate.postForEntity("/v1/tasks/complete?taskId=" + taskId1, null, String.class);

        when(context.getUsername()).thenReturn("bob");
        when(context.getGroups()).thenReturn(List.of("cuistos"));

        ResponseEntity<String> rawResponse2 = restTemplate.getForEntity("/v1/tasks", String.class);
        System.out.println("Raw JSON for Bob: " + rawResponse2.getBody());
        assertEquals(HttpStatus.OK, rawResponse2.getStatusCode());

        ResponseEntity<List<TaskInstance>> taskResponse2 = restTemplate.exchange(
                "/v1/tasks",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        List<TaskInstance> tasksForBob = taskResponse2.getBody();
        assertNotNull(tasksForBob);
        assertFalse(tasksForBob.isEmpty());

        String taskId2 = tasksForBob.get(0).getId();
        restTemplate.postForEntity("/v1/tasks/claim?taskId=" + taskId2, null, String.class);
        restTemplate.postForEntity("/v1/tasks/complete?taskId=" + taskId2, null, String.class);

        ResponseEntity<String> rawFinal = restTemplate.getForEntity("/v1/tasks", String.class);
        System.out.println("Raw JSON for final task check: " + rawFinal.getBody());

        ResponseEntity<List<TaskInstance>> finalTasks = restTemplate.exchange(
                "/v1/tasks",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        List<TaskInstance> remainingTasks = finalTasks.getBody();
        assertNotNull(remainingTasks);
        assertEquals(0, remainingTasks.size());
    }
}
