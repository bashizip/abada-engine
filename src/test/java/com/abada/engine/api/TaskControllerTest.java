package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.model.TaskStatus;
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
    @DisplayName("Should allow a user to list, view, claim, and complete their tasks with correct status transitions")
    void shouldExecuteFullTaskLifecycleViaApi() {
        // --- Step 1: Start process ---
        abadaEngine.startProcess("recipe-cook");

        // --- Step 2: Alice lists her tasks and verifies the status is AVAILABLE ---
        HttpHeaders aliceHeaders = new HttpHeaders();
        aliceHeaders.set("X-User", "alice");
        aliceHeaders.set("X-Groups", "customers");
        HttpEntity<Void> aliceRequestEntity = new HttpEntity<>(aliceHeaders);

        ResponseEntity<List<TaskDetailsDto>> listResponse = restTemplate.exchange(
                "/v1/tasks", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {}
        );
        assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(listResponse.getBody()).isNotNull().hasSize(1);
        TaskDetailsDto initialTask = listResponse.getBody().get(0);
        String taskId = initialTask.id();
        assertThat(initialTask.status()).isEqualTo(TaskStatus.AVAILABLE);

        // --- Step 3: Alice gets details for her task ---
        ResponseEntity<TaskDetailsDto> detailsResponse = restTemplate.exchange(
                "/v1/tasks/{id}", HttpMethod.GET, aliceRequestEntity, TaskDetailsDto.class, taskId
        );
        assertThat(detailsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detailsResponse.getBody()).isNotNull();
        assertThat(detailsResponse.getBody().id()).isEqualTo(taskId);
        assertThat(detailsResponse.getBody().name()).isEqualTo("Choose Recipe");
        assertThat(detailsResponse.getBody().status()).isEqualTo(TaskStatus.AVAILABLE);

        // --- Step 4: Alice claims the task and verifies the status is CLAIMED ---
        ResponseEntity<Map> claimResponse = restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Map.class, taskId);
        assertThat(claimResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(claimResponse.getBody().get("status")).isEqualTo("Claimed");

        // Verify status is updated to CLAIMED
        ResponseEntity<TaskDetailsDto> claimedDetailsResponse = restTemplate.exchange("/v1/tasks/{id}", HttpMethod.GET, aliceRequestEntity, TaskDetailsDto.class, taskId);
        assertThat(claimedDetailsResponse.getBody().status()).isEqualTo(TaskStatus.CLAIMED);
        assertThat(claimedDetailsResponse.getBody().assignee()).isEqualTo("alice");

        // --- Step 5: Alice completes the task ---
        HttpEntity<Map<String, Object>> completeRequest = new HttpEntity<>(Map.of("goodOne", true), aliceHeaders);
        ResponseEntity<Map> completeResponse = restTemplate.exchange("/v1/tasks/complete?taskId={taskId}", HttpMethod.POST, completeRequest, Map.class, taskId);
        assertThat(completeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(completeResponse.getBody().get("status")).isEqualTo("Completed");

        // --- Step 6: Switch user to Bob and verify the next task is visible ---
        HttpHeaders bobHeaders = new HttpHeaders();
        bobHeaders.set("X-User", "bob");
        bobHeaders.set("X-Groups", "cuistos");
        HttpEntity<Void> bobRequestEntity = new HttpEntity<>(bobHeaders);

        ResponseEntity<List<TaskDetailsDto>> bobListResponse = restTemplate.exchange(
                "/v1/tasks", HttpMethod.GET, bobRequestEntity, new ParameterizedTypeReference<>() {}
        );
        assertThat(bobListResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(bobListResponse.getBody()).isNotNull().hasSize(1);
        assertThat(bobListResponse.getBody().get(0).name()).isEqualTo("Cook Recipe");
        assertThat(bobListResponse.getBody().get(0).status()).isEqualTo(TaskStatus.AVAILABLE);
    }

    @Test
    @DisplayName("GET /v1/tasks should filter tasks by status")
    void shouldFilterTasksByStatus() {
        // --- Step 1: Start process to create an AVAILABLE task ---
        abadaEngine.startProcess("recipe-cook");
        HttpHeaders aliceHeaders = new HttpHeaders();
        aliceHeaders.set("X-User", "alice");
        aliceHeaders.set("X-Groups", "customers");
        HttpEntity<Void> aliceRequestEntity = new HttpEntity<>(aliceHeaders);

        // --- Step 2: Verify filtering for AVAILABLE status ---
        ResponseEntity<List<TaskDetailsDto>> availableResponse = restTemplate.exchange("/v1/tasks?status=AVAILABLE", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {});
        assertThat(availableResponse.getBody()).hasSize(1);
        assertThat(availableResponse.getBody().get(0).name()).isEqualTo("Choose Recipe");

        // --- Step 3: Verify filtering for CLAIMED status (should be empty) ---
        ResponseEntity<List<TaskDetailsDto>> claimedEmptyResponse = restTemplate.exchange("/v1/tasks?status=CLAIMED", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {});
        assertThat(claimedEmptyResponse.getBody()).isNotNull().isEmpty();

        // --- Step 4: Claim the task ---
        String taskId = availableResponse.getBody().get(0).id();
        restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Map.class, taskId);

        // --- Step 5: Verify filtering for CLAIMED status again ---
        ResponseEntity<List<TaskDetailsDto>> claimedResponse = restTemplate.exchange("/v1/tasks?status=CLAIMED", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {});
        assertThat(claimedResponse.getBody()).hasSize(1);
        assertThat(claimedResponse.getBody().get(0).id()).isEqualTo(taskId);

        // --- Step 6: Verify filtering for AVAILABLE status again (should be empty) ---
        ResponseEntity<List<TaskDetailsDto>> availableEmptyResponse = restTemplate.exchange("/v1/tasks?status=AVAILABLE", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {});
        assertThat(availableEmptyResponse.getBody()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("POST /v1/tasks/fail should mark a task as FAILED")
    void shouldFailTask() {
        // --- Step 1: Start process and get the task ID ---
        abadaEngine.startProcess("recipe-cook");
        HttpHeaders aliceHeaders = new HttpHeaders();
        aliceHeaders.set("X-User", "alice");
        aliceHeaders.set("X-Groups", "customers");
        HttpEntity<Void> aliceRequestEntity = new HttpEntity<>(aliceHeaders);
        ResponseEntity<List<TaskDetailsDto>> listResponse = restTemplate.exchange("/v1/tasks", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {});
        String taskId = listResponse.getBody().get(0).id();

        // --- Step 2: Call the fail endpoint ---
        ResponseEntity<Map> failResponse = restTemplate.exchange("/v1/tasks/fail?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Map.class, taskId);
        assertThat(failResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(failResponse.getBody().get("status")).isEqualTo("Failed");
        assertThat(failResponse.getBody().get("taskId")).isEqualTo(taskId);

        // --- Step 3: Verify the task status is now FAILED ---
        ResponseEntity<TaskDetailsDto> detailsResponse = restTemplate.exchange("/v1/tasks/{id}", HttpMethod.GET, aliceRequestEntity, TaskDetailsDto.class, taskId);
        assertThat(detailsResponse.getBody().status()).isEqualTo(TaskStatus.FAILED);
    }
}
