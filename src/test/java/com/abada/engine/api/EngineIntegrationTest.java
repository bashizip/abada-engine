package com.abada.engine.api;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.dto.ProcessInstanceDTO;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EngineIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AbadaEngine abadaEngine;

    private HttpHeaders httpHeaders;


    private String instanceId;

    @BeforeEach
    void setup() throws IOException {
        abadaEngine.clearMemory();

        httpHeaders = new HttpHeaders();
        httpHeaders.set("X-User", "alice");
        httpHeaders.set("X-Groups", "customers");

        instanceId = deployAndStartProcess();
    }

    private String deployAndStartProcess() throws IOException {
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
        ResponseEntity<String> response = restTemplate.postForEntity("/v1/processes/start", startRequest, String.class);
        return response.getBody().replace("Started instance: ", "");
    }

    @Test
    void shouldCompleteAllTasksAndFinishProcess() {
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

        HttpHeaders completeHeaders1 = new HttpHeaders();
        completeHeaders1.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> completeRequest1 = new HttpEntity<>(Map.of("goodOne", true), completeHeaders1);
        restTemplate.postForEntity("/v1/tasks/complete?taskId=" + taskId1, completeRequest1, String.class);

        httpHeaders.set("X-User", "bob");
        httpHeaders.set("X-Groups", "cuistos");

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


        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> completeRequest2 = new HttpEntity<>(Collections.emptyMap(), httpHeaders);
        restTemplate.postForEntity("/v1/tasks/complete?taskId=" + taskId2, completeRequest2, String.class);

        ResponseEntity<List<TaskInstance>> finalTasks = restTemplate.exchange(
                "/v1/tasks",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        List<TaskInstance> remainingTasks = finalTasks.getBody();
        assertNotNull(remainingTasks);
        assertEquals(0, remainingTasks.size());

        ResponseEntity<ProcessInstanceDTO> instanceResponse = restTemplate.getForEntity("/v1/processes/instance/" + instanceId, ProcessInstanceDTO.class);
        assertNotNull(instanceResponse.getBody());
        assertTrue(instanceResponse.getBody().isCompleted());
    }
}
