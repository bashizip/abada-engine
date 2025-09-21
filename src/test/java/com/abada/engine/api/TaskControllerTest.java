package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.dto.TaskDetailsDto;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
public class TaskControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AbadaEngine abadaEngine;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        // Deploy the process once for all tests in this class
        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    @Test
    @DisplayName("Should allow a user to list, view, claim, and complete their tasks")
    void shouldExecuteFullTaskLifecycleViaApi() {
        // --- Step 1: Start process ---
        abadaEngine.startProcess("recipe-cook");

        // --- Step 2: Alice lists her tasks ---
        HttpHeaders aliceHeaders = new HttpHeaders();
        aliceHeaders.set("X-User", "alice");
        aliceHeaders.set("X-Groups", "customers");
        HttpEntity<Void> aliceRequestEntity = new HttpEntity<>(aliceHeaders);

        ResponseEntity<List<TaskDetailsDto>> listResponse = restTemplate.exchange(
                "/v1/tasks", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<List<TaskDetailsDto>>() {}
        );
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull().hasSize(1);
        String taskId = listResponse.getBody().get(0).id();

        // --- Step 3: Alice gets details for her task ---
        ResponseEntity<TaskDetailsDto> detailsResponse = restTemplate.exchange(
                "/v1/tasks/{id}", HttpMethod.GET, aliceRequestEntity, TaskDetailsDto.class, taskId
        );
        assertThat(detailsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailsResponse.getBody()).isNotNull();
        assertThat(detailsResponse.getBody().id()).isEqualTo(taskId);
        assertThat(detailsResponse.getBody().name()).isEqualTo("Choose Recipe");

        // --- Step 4: Alice claims and completes the task ---
        ResponseEntity<Void> claimResponse = restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Void.class, taskId);
        assertThat(claimResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        HttpEntity<Map<String, Object>> completeRequest = new HttpEntity<>(Map.of("goodOne", true), aliceHeaders);
        ResponseEntity<Void> completeResponse = restTemplate.exchange("/v1/tasks/complete?taskId={taskId}", HttpMethod.POST, completeRequest, Void.class, taskId);
        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        // --- Step 5: Switch user to Bob and verify the next task is visible ---
        HttpHeaders bobHeaders = new HttpHeaders();
        bobHeaders.set("X-User", "bob");
        bobHeaders.set("X-Groups", "cuistos");
        HttpEntity<Void> bobRequestEntity = new HttpEntity<>(bobHeaders);

        ResponseEntity<List<TaskDetailsDto>> bobListResponse = restTemplate.exchange(
                "/v1/tasks", HttpMethod.GET, bobRequestEntity, new ParameterizedTypeReference<List<TaskDetailsDto>>() {}
        );
        assertThat(bobListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bobListResponse.getBody()).isNotNull().hasSize(1);
        assertThat(bobListResponse.getBody().get(0).name()).isEqualTo("Cook Recipe");
    }
}
