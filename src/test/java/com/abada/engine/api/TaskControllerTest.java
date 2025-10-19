package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.model.TaskStatus;
import com.abada.engine.dto.ErrorResponse;
import com.abada.engine.dto.TaskDetailsDto;
import com.abada.engine.dto.UserStatsDto;
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

    @Test
    @DisplayName("GET /v1/tasks/user-stats should return comprehensive user statistics")
    void shouldReturnUserStats() {
        // --- Step 1: Start multiple processes to create test data ---
        abadaEngine.startProcess("recipe-cook");
        abadaEngine.startProcess("recipe-cook");
        abadaEngine.startProcess("recipe-cook");

        HttpHeaders aliceHeaders = new HttpHeaders();
        aliceHeaders.set("X-User", "alice");
        aliceHeaders.set("X-Groups", "customers");
        HttpEntity<Void> aliceRequestEntity = new HttpEntity<>(aliceHeaders);

        // --- Step 2: Get initial tasks and claim one ---
        ResponseEntity<List<TaskDetailsDto>> listResponse = restTemplate.exchange(
                "/v1/tasks", HttpMethod.GET, aliceRequestEntity, new ParameterizedTypeReference<>() {}
        );
        assertThat(listResponse.getBody()).isNotNull().hasSize(3); // 3 AVAILABLE tasks

        String taskId1 = listResponse.getBody().get(0).id();
        String taskId2 = listResponse.getBody().get(1).id();

        // Claim first task
        restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Map.class, taskId1);

        // Complete second task
        restTemplate.exchange("/v1/tasks/claim?taskId={taskId}", HttpMethod.POST, aliceRequestEntity, Map.class, taskId2);
        HttpEntity<Map<String, Object>> completeRequest = new HttpEntity<>(Map.of("goodOne", true), aliceHeaders);
        restTemplate.exchange("/v1/tasks/complete?taskId={taskId}", HttpMethod.POST, completeRequest, Map.class, taskId2);

        // --- Step 3: Call the user stats endpoint ---
        ResponseEntity<UserStatsDto> statsResponse = restTemplate.exchange(
                "/v1/tasks/user-stats", HttpMethod.GET, aliceRequestEntity, UserStatsDto.class
        );

        // --- Step 4: Verify the response structure and data ---
        assertThat(statsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statsResponse.getBody()).isNotNull();

        UserStatsDto stats = statsResponse.getBody();

        // Verify quick stats
        assertThat(stats.quickStats()).isNotNull();
        assertThat(stats.quickStats().activeTasks()).isGreaterThanOrEqualTo(1); // At least 1 CLAIMED task
        assertThat(stats.quickStats().completedTasks()).isGreaterThanOrEqualTo(1); // At least 1 COMPLETED task
        assertThat(stats.quickStats().runningProcesses()).isGreaterThanOrEqualTo(3); // At least 3 running processes
        assertThat(stats.quickStats().availableTasks()).isGreaterThanOrEqualTo(1); // At least 1 AVAILABLE task

        // Verify recent tasks
        assertThat(stats.recentTasks()).isNotNull();
        assertThat(stats.recentTasks()).hasSizeGreaterThanOrEqualTo(2); // At least 2 tasks assigned to alice
        assertThat(stats.recentTasks().get(0).name()).isEqualTo("Choose Recipe");

        // Verify tasks by status
        assertThat(stats.tasksByStatus()).isNotNull();
        assertThat(stats.tasksByStatus().get(TaskStatus.CLAIMED)).isGreaterThanOrEqualTo(1);
        assertThat(stats.tasksByStatus().get(TaskStatus.COMPLETED)).isGreaterThanOrEqualTo(1);

        // Verify overdue tasks (should be empty for new tasks)
        assertThat(stats.overdueTasks()).isNotNull();
        assertThat(stats.overdueTasks()).isEmpty();

        // Verify process activity
        assertThat(stats.processActivity()).isNotNull();
        assertThat(stats.processActivity().recentlyStartedProcesses()).isNotNull();
        assertThat(stats.processActivity().recentlyStartedProcesses()).hasSizeGreaterThanOrEqualTo(3); // At least 3 processes
        assertThat(stats.processActivity().activeProcessCount()).isGreaterThanOrEqualTo(3); // At least 3 active processes
        assertThat(stats.processActivity().completionRate()).isGreaterThanOrEqualTo(0.0); // Completion rate >= 0
    }

    @Test
    @DisplayName("GET /v1/tasks/user-stats should return empty stats for user with no tasks")
    void shouldReturnEmptyStatsForUserWithNoTasks() {
        // --- Step 1: Start a process but don't assign any tasks to charlie ---
        abadaEngine.startProcess("recipe-cook");

        HttpHeaders charlieHeaders = new HttpHeaders();
        charlieHeaders.set("X-User", "charlie");
        charlieHeaders.set("X-Groups", "other-group"); // Different group, no access to tasks
        HttpEntity<Void> charlieRequestEntity = new HttpEntity<>(charlieHeaders);

        // --- Step 2: Call the user stats endpoint ---
        ResponseEntity<UserStatsDto> statsResponse = restTemplate.exchange(
                "/v1/tasks/user-stats", HttpMethod.GET, charlieRequestEntity, UserStatsDto.class
        );

        // --- Step 3: Verify the response has empty/zero stats ---
        assertThat(statsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(statsResponse.getBody()).isNotNull();

        UserStatsDto stats = statsResponse.getBody();

        // Verify quick stats are zero
        assertThat(stats.quickStats().activeTasks()).isEqualTo(0);
        assertThat(stats.quickStats().completedTasks()).isEqualTo(0);
        assertThat(stats.quickStats().runningProcesses()).isEqualTo(0);
        assertThat(stats.quickStats().availableTasks()).isEqualTo(0);

        // Verify recent tasks is empty
        assertThat(stats.recentTasks()).isEmpty();

        // Verify tasks by status is empty
        assertThat(stats.tasksByStatus()).isEmpty();

        // Verify overdue tasks is empty
        assertThat(stats.overdueTasks()).isEmpty();

        // Verify process activity is empty
        assertThat(stats.processActivity().recentlyStartedProcesses()).isEmpty();
        assertThat(stats.processActivity().activeProcessCount()).isEqualTo(0);
        assertThat(stats.processActivity().completionRate()).isEqualTo(0.0);
    }
}
