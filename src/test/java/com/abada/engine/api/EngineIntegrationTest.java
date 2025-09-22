package com.abada.engine.api;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.dto.ProcessInstanceDTO;
import com.abada.engine.dto.TaskDetailsDto;
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

    private String instanceId;
    private HttpHeaders aliceHeaders;
    private HttpHeaders bobHeaders;

    @BeforeEach
    void setup() throws IOException {
        abadaEngine.clearMemory();

        aliceHeaders = new HttpHeaders();
        aliceHeaders.set("X-User", "alice");
        aliceHeaders.set("X-Groups", "customers");

        bobHeaders = new HttpHeaders();
        bobHeaders.set("X-User", "bob");
        bobHeaders.set("X-Groups", "cuistos");

        instanceId = deployAndStartProcess();
    }

    private String deployAndStartProcess() throws IOException {
        HttpHeaders deployHeaders = new HttpHeaders();
        deployHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        deployHeaders.addAll(aliceHeaders);

        ByteArrayResource file = new ByteArrayResource(
                BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn").readAllBytes()) {
            @Override
            public String getFilename() {
                return "recipe-cook.bpmn";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, deployHeaders);
        // Correctly expect a Map from the /deploy endpoint
        restTemplate.postForEntity("/v1/processes/deploy", request, Map.class);

        HttpHeaders startHeaders = new HttpHeaders();
        startHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        startHeaders.addAll(aliceHeaders);

        HttpEntity<String> startRequest = new HttpEntity<>("processId=recipe-cook", startHeaders);
        // Correctly expect a Map from the /start endpoint
        ResponseEntity<Map> response = restTemplate.postForEntity("/v1/processes/start", startRequest, Map.class);
        
        assertNotNull(response.getBody());
        // Correctly extract the processInstanceId from the JSON response
        return (String) response.getBody().get("processInstanceId");
    }

    @Test
    void shouldCompleteAllTasksAndFinishProcess() {
        // Alice's turn
        HttpEntity<Void> aliceRequest = new HttpEntity<>(aliceHeaders);
        ResponseEntity<List<TaskDetailsDto>> taskResponse1 = restTemplate.exchange(
                "/v1/tasks", HttpMethod.GET, aliceRequest, new ParameterizedTypeReference<>() {}
        );
        List<TaskDetailsDto> tasksForAlice = taskResponse1.getBody();
        assertNotNull(tasksForAlice);
        assertFalse(tasksForAlice.isEmpty());

        String taskId1 = tasksForAlice.get(0).id();
        // Correctly expect a Map from the /claim endpoint
        restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, aliceRequest, Map.class, taskId1);

        HttpHeaders completeHeaders1 = new HttpHeaders(aliceHeaders);
        completeHeaders1.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> completeRequest1 = new HttpEntity<>(Map.of("goodOne", true), completeHeaders1);
        // Correctly expect a Map from the /complete endpoint
        restTemplate.postForEntity("/v1/tasks/complete?taskId={taskId}", completeRequest1, Map.class, taskId1);

        // Bob's turn
        HttpEntity<Void> bobRequest = new HttpEntity<>(bobHeaders);
        ResponseEntity<List<TaskDetailsDto>> taskResponse2 = restTemplate.exchange(
                "/v1/tasks", HttpMethod.GET, bobRequest, new ParameterizedTypeReference<>() {}
        );
        List<TaskDetailsDto> tasksForBob = taskResponse2.getBody();
        assertNotNull(tasksForBob);
        assertFalse(tasksForBob.isEmpty());

        String taskId2 = tasksForBob.get(0).id();
        // Correctly expect a Map from the /claim endpoint
        restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, bobRequest, Map.class, taskId2);

        HttpHeaders completeHeaders2 = new HttpHeaders(bobHeaders);
        completeHeaders2.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> completeRequest2 = new HttpEntity<>(Collections.emptyMap(), completeHeaders2);
        // Correctly expect a Map from the /complete endpoint
        restTemplate.postForEntity("/v1/tasks/complete?taskId={taskId}", completeRequest2, Map.class, taskId2);

        // Final check
        ResponseEntity<List<TaskDetailsDto>> finalTasks = restTemplate.exchange(
                "/v1/tasks", HttpMethod.GET, bobRequest, new ParameterizedTypeReference<>() {}
        );
        List<TaskDetailsDto> remainingTasks = finalTasks.getBody();
        assertNotNull(remainingTasks);
        assertEquals(0, remainingTasks.size());

        // Correctly provide the instanceId as a variable for the URL template
        ResponseEntity<ProcessInstanceDTO> instanceResponse = restTemplate.exchange(
                "/v1/processes/instance/{id}", HttpMethod.GET, bobRequest, ProcessInstanceDTO.class, instanceId
        );
        assertNotNull(instanceResponse.getBody());
        assertTrue(instanceResponse.getBody().isCompleted());
    }
}
