package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.model.TaskStatus;
import com.abada.engine.dto.ErrorResponse;
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
    @DisplayName("Should execute full task lifecycle with correct status and date transitions")
    void shouldExecuteFullTaskLifecycleViaApi() {
        // --- Step 1: Start process ---
        abadaEngine.startProcess("recipe-cook");

        // --- Step 2: Alice lists her tasks and verifies status and dates ---
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
        assertThat(initialTask.startDate()).isNotNull();
        assertThat(initialTask.endDate()).isNull();

        // --- Step 3: Alice gets details and verifies dates ---
        ResponseEntity<TaskDetailsDto> detailsResponse = restTemplate.exchange(
                "/v1/tasks/{id}", HttpMethod.GET, aliceRequestEntity, TaskDetailsDto.class, taskId
        );
        assertThat(detailsResponse.getBody().startDate()).isNotNull();
        assertThat(detailsResponse.getBody().endDate()).isNull();

        // --- Step 4: Alice claims the task ---
        restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Map.class, taskId);

        // --- Step 5: Alice completes the task ---
        HttpEntity<Map<String, Object>> completeRequest = new HttpEntity<>(Map.of("goodOne", true), aliceHeaders);
        restTemplate.exchange("/v1/tasks/complete?taskId={taskId}", HttpMethod.POST, completeRequest, Map.class, taskId);

        // Verify endDate is set after completion
        ResponseEntity<TaskDetailsDto> completedDetailsResponse = restTemplate.exchange("/v1/tasks/{id}", HttpMethod.GET, aliceRequestEntity, TaskDetailsDto.class, taskId);
        assertThat(completedDetailsResponse.getBody().status()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(completedDetailsResponse.getBody().endDate()).isNotNull();

        // --- Step 6: Bob verifies the next task has a start date ---
        HttpHeaders bobHeaders = new HttpHeaders();
        bobHeaders.set("X-User", "bob");
        bobHeaders.set("X-Groups", "cuistos");
        HttpEntity<Void> bobRequestEntity = new HttpEntity<>(bobHeaders);

        ResponseEntity<List<TaskDetailsDto>> bobListResponse = restTemplate.exchange(
                "/v1/tasks", HttpMethod.GET, bobRequestEntity, new ParameterizedTypeReference<>() {}
        );
        assertThat(bobListResponse.getBody()).isNotNull().hasSize(1);
        assertThat(bobListResponse.getBody().get(0).startDate()).isNotNull();
        assertThat(bobListResponse.getBody().get(0).endDate()).isNull();
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

        // --- Step 3: Verify filtering for CLAIMED status (should be empty) ---
        ResponseEntity<List<TaskDetailsDto>> claimedEmptyResponse = restTemplate.exchange("/v1/tasks?status=CLAIMED", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {});
        assertThat(claimedEmptyResponse.getBody()).isNotNull().isEmpty();

        // --- Step 4: Claim the task ---
        String taskId = availableResponse.getBody().get(0).id();
        restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Map.class, taskId);

        // --- Step 5: Verify filtering for CLAIMED status again ---
        ResponseEntity<List<TaskDetailsDto>> claimedResponse = restTemplate.exchange("/v1/tasks?status=CLAIMED", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {});
        assertThat(claimedResponse.getBody()).hasSize(1);

        // --- Step 6: Verify filtering for AVAILABLE status again (should be empty) ---
        ResponseEntity<List<TaskDetailsDto>> availableEmptyResponse = restTemplate.exchange("/v1/tasks?status=AVAILABLE", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {});
        assertThat(availableEmptyResponse.getBody()).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("POST /v1/tasks/fail should mark a task as FAILED and set endDate")
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
        restTemplate.exchange("/v1/tasks/fail?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Map.class, taskId);

        // --- Step 3: Verify the task status and dates are now set ---
        ResponseEntity<TaskDetailsDto> detailsResponse = restTemplate.exchange("/v1/tasks/{id}", HttpMethod.GET, aliceRequestEntity, TaskDetailsDto.class, taskId);
        assertThat(detailsResponse.getBody().status()).isEqualTo(TaskStatus.FAILED);
        assertThat(detailsResponse.getBody().startDate()).isNotNull();
        assertThat(detailsResponse.getBody().endDate()).isNotNull();
    }

    @Test
    @DisplayName("Should return 400 error when claiming an already claimed task")
    void shouldReturnErrorWhenClaimingUnavailableTask() {
        // --- Step 1: Start process and have Alice claim the task ---
        abadaEngine.startProcess("recipe-cook");
        HttpHeaders aliceHeaders = new HttpHeaders();
        aliceHeaders.set("X-User", "alice");
        aliceHeaders.set("X-Groups", "customers");
        HttpEntity<Void> aliceRequestEntity = new HttpEntity<>(aliceHeaders);
        ResponseEntity<List<TaskDetailsDto>> listResponse = restTemplate.exchange("/v1/tasks", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {});
        String taskId = listResponse.getBody().get(0).id();
        restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Map.class, taskId);

        // --- Step 2: Have Bob try to claim the same task ---
        HttpHeaders bobHeaders = new HttpHeaders();
        bobHeaders.set("X-User", "bob");
        bobHeaders.set("X-Groups", "customers");
        HttpEntity<Void> bobRequestEntity = new HttpEntity<>(bobHeaders);
        ResponseEntity<ErrorResponse> errorResponse = restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, bobRequestEntity, ErrorResponse.class, taskId);

        // --- Step 3: Verify the error response ---
        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorResponse.getBody()).isNotNull();
        assertThat(errorResponse.getBody().message()).contains("Task is not available to be claimed");
    }

    @Test
    @DisplayName("Should return 400 error when completing a task assigned to another user")
    void shouldReturnErrorWhenCompletingUnassignedTask() {
        // --- Step 1: Start process and have Alice claim the task ---
        abadaEngine.startProcess("recipe-cook");
        HttpHeaders aliceHeaders = new HttpHeaders();
        aliceHeaders.set("X-User", "alice");
        aliceHeaders.set("X-Groups", "customers");
        HttpEntity<Void> aliceRequestEntity = new HttpEntity<>(aliceHeaders);
        ResponseEntity<List<TaskDetailsDto>> listResponse = restTemplate.exchange("/v1/tasks", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {});
        String taskId = listResponse.getBody().get(0).id();
        restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Map.class, taskId);

        // --- Step 2: Have Bob (unauthorized) try to complete the task ---
        HttpHeaders bobHeaders = new HttpHeaders();
        bobHeaders.set("X-User", "bob");
        bobHeaders.set("X-Groups", "customers");
        HttpEntity<Map<String, Object>> bobCompleteRequest = new HttpEntity<>(Map.of("goodOne", true), bobHeaders);
        ResponseEntity<ErrorResponse> errorResponse = restTemplate.exchange("/v1/tasks/complete?taskId={taskId}", HttpMethod.POST, bobCompleteRequest, ErrorResponse.class, taskId);

        // --- Step 3: Verify the error response ---
        assertThat(errorResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(errorResponse.getBody()).isNotNull();
        assertThat(errorResponse.getBody().message()).contains("is not authorized to complete task");
    }
}
