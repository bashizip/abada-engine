package com.abada.engine.core;

import com.abada.engine.core.model.ProcessStatus;

import com.abada.engine.dto.ProcessInstanceDTO;
import com.abada.engine.dto.SuspensionRequest;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for process instance suspension functionality.
 * Tests that suspended processes cannot advance through task completion,
 * event triggering, or any other execution mechanism.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ProcessSuspensionTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AbadaEngine abadaEngine;

    private String instanceId;
    private HttpHeaders aliceHeaders;

    @BeforeEach
    void setup() throws IOException {
        abadaEngine.clearMemory();

        aliceHeaders = new HttpHeaders();
        aliceHeaders.set("X-User", "alice");
        aliceHeaders.set("X-Groups", "customers");

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
        restTemplate.postForEntity("/v1/processes/deploy", request, Map.class);

        HttpEntity<Void> startRequest = new HttpEntity<>(aliceHeaders);
        ResponseEntity<Map> response = restTemplate.postForEntity("/v1/processes/start?processId=recipe-cook",
                startRequest,
                Map.class);

        assertNotNull(response.getBody());
        return (String) response.getBody().get("processInstanceId");
    }

    @Test
    void shouldSuspendProcessInstance() {
        // Suspend the process
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SuspensionRequest> suspendRequest = new HttpEntity<>(new SuspensionRequest(true), headers);

        ResponseEntity<Void> suspendResponse = restTemplate.exchange(
                "/v1/process-instances/" + instanceId + "/suspension",
                HttpMethod.PUT,
                suspendRequest,
                Void.class);

        assertEquals(HttpStatus.OK, suspendResponse.getStatusCode());

        // Verify the process is suspended
        ResponseEntity<ProcessInstanceDTO> getResponse = restTemplate.getForEntity(
                "/v1/processes/instances/" + instanceId,
                ProcessInstanceDTO.class);

        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        assertNotNull(getResponse.getBody());
        assertTrue(getResponse.getBody().suspended(), "Process should be suspended");
        assertEquals(ProcessStatus.SUSPENDED, getResponse.getBody().status(),
                "Suspended process should have SUSPENDED status");
    }

    @Test
    void shouldResumeProcessInstance() {
        // First suspend
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SuspensionRequest> suspendRequest = new HttpEntity<>(new SuspensionRequest(true), headers);
        restTemplate.put("/v1/process-instances/" + instanceId + "/suspension", suspendRequest);

        // Verify suspended
        ProcessInstanceDTO suspendedInstance = restTemplate.getForEntity(
                "/v1/processes/instances/" + instanceId,
                ProcessInstanceDTO.class).getBody();
        assertTrue(suspendedInstance.suspended());

        // Now resume
        HttpEntity<SuspensionRequest> resumeRequest = new HttpEntity<>(new SuspensionRequest(false), headers);
        ResponseEntity<Void> resumeResponse = restTemplate.exchange(
                "/v1/process-instances/" + instanceId + "/suspension",
                HttpMethod.PUT,
                resumeRequest,
                Void.class);

        assertEquals(HttpStatus.OK, resumeResponse.getStatusCode());

        // Verify the process is no longer suspended
        ProcessInstanceDTO resumedInstance = restTemplate.getForEntity(
                "/v1/processes/instances/" + instanceId,
                ProcessInstanceDTO.class).getBody();

        assertFalse(resumedInstance.suspended(), "Process should not be suspended");
        assertEquals(ProcessStatus.RUNNING, resumedInstance.status(),
                "Resumed process should have RUNNING status");
    }

    @Test
    void shouldPreventTaskCompletionWhenSuspended() {
        // Get the first task
        HttpEntity<Void> request = new HttpEntity<>(aliceHeaders);
        ResponseEntity<List<TaskDetailsDto>> taskResponse = restTemplate.exchange(
                "/v1/tasks",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {
                });

        assertNotNull(taskResponse.getBody());
        assertFalse(taskResponse.getBody().isEmpty());
        String taskId = taskResponse.getBody().get(0).id();

        // Claim the task
        restTemplate.postForEntity("/v1/tasks/claim?taskId=" + taskId + "&user=alice", null, Map.class);

        // Suspend the process
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SuspensionRequest> suspendRequest = new HttpEntity<>(new SuspensionRequest(true), headers);
        restTemplate.put("/v1/process-instances/" + instanceId + "/suspension", suspendRequest);

        // Try to complete the task - should fail
        HttpHeaders completeHeaders = new HttpHeaders(aliceHeaders);
        completeHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> completeRequest = new HttpEntity<>(
                Map.of("goodOne", true),
                completeHeaders);

        ResponseEntity<Map> completeResponse = restTemplate.postForEntity(
                "/v1/tasks/complete?taskId=" + taskId,
                completeRequest,
                Map.class);

        assertEquals(HttpStatus.BAD_REQUEST, completeResponse.getStatusCode(),
                "Task completion should fail when process is suspended");
    }

    @Test
    void shouldAllowTaskCompletionAfterResumption() {
        // Get and claim the task
        HttpEntity<Void> request = new HttpEntity<>(aliceHeaders);
        ResponseEntity<List<TaskDetailsDto>> taskResponse = restTemplate.exchange(
                "/v1/tasks",
                HttpMethod.GET,
                request,
                new ParameterizedTypeReference<>() {
                });

        String taskId = taskResponse.getBody().get(0).id();
        restTemplate.postForEntity("/v1/tasks/claim?taskId=" + taskId + "&user=alice", null, Map.class);

        // Suspend the process
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SuspensionRequest> suspendRequest = new HttpEntity<>(new SuspensionRequest(true), headers);
        restTemplate.put("/v1/process-instances/" + instanceId + "/suspension", suspendRequest);

        // Resume the process
        HttpEntity<SuspensionRequest> resumeRequest = new HttpEntity<>(new SuspensionRequest(false), headers);
        restTemplate.put("/v1/process-instances/" + instanceId + "/suspension", resumeRequest);

        // Now complete the task - should succeed
        HttpHeaders completeHeaders = new HttpHeaders(aliceHeaders);
        completeHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> completeRequest = new HttpEntity<>(
                Map.of("goodOne", true),
                completeHeaders);

        ResponseEntity<Map> completeResponse = restTemplate.postForEntity(
                "/v1/tasks/complete?taskId=" + taskId,
                completeRequest,
                Map.class);

        assertEquals(HttpStatus.OK, completeResponse.getStatusCode(),
                "Task completion should succeed after resumption");
    }

    @Test
    void shouldHandleMultipleSuspensionToggles() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Suspend
        HttpEntity<SuspensionRequest> suspendRequest = new HttpEntity<>(new SuspensionRequest(true), headers);
        restTemplate.put("/v1/process-instances/" + instanceId + "/suspension", suspendRequest);
        ProcessInstanceDTO instance = getProcessInstance();
        assertTrue(instance.suspended());
        assertEquals(ProcessStatus.SUSPENDED, instance.status());

        // Resume
        HttpEntity<SuspensionRequest> resumeRequest = new HttpEntity<>(new SuspensionRequest(false), headers);
        restTemplate.put("/v1/process-instances/" + instanceId + "/suspension", resumeRequest);
        instance = getProcessInstance();
        assertFalse(instance.suspended());
        assertEquals(ProcessStatus.RUNNING, instance.status());

        // Suspend again
        restTemplate.put("/v1/process-instances/" + instanceId + "/suspension", suspendRequest);
        instance = getProcessInstance();
        assertTrue(instance.suspended());
        assertEquals(ProcessStatus.SUSPENDED, instance.status());

        // Resume again
        restTemplate.put("/v1/process-instances/" + instanceId + "/suspension", resumeRequest);
        instance = getProcessInstance();
        assertFalse(instance.suspended());
        assertEquals(ProcessStatus.RUNNING, instance.status());
    }

    private ProcessInstanceDTO getProcessInstance() {
        return restTemplate.getForEntity(
                "/v1/processes/instances/" + instanceId,
                ProcessInstanceDTO.class).getBody();
    }

    @Test
    void shouldReturnNotFoundForNonExistentProcess() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SuspensionRequest> suspendRequest = new HttpEntity<>(new SuspensionRequest(true), headers);

        ResponseEntity<Void> response = restTemplate.exchange(
                "/v1/process-instances/non-existent-id/suspension",
                HttpMethod.PUT,
                suspendRequest,
                Void.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldPersistSuspensionState() {
        // Suspend the process
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SuspensionRequest> suspendRequest = new HttpEntity<>(new SuspensionRequest(true), headers);
        restTemplate.put("/v1/process-instances/" + instanceId + "/suspension", suspendRequest);

        // Retrieve the process instance multiple times to verify persistence
        for (int i = 0; i < 3; i++) {
            ProcessInstanceDTO instance = restTemplate.getForEntity(
                    "/v1/processes/instances/" + instanceId,
                    ProcessInstanceDTO.class).getBody();

            assertTrue(instance.suspended(),
                    "Suspension state should persist across multiple retrievals");
            assertEquals(ProcessStatus.SUSPENDED, instance.status(),
                    "SUSPENDED status should persist across multiple retrievals");
        }
    }
}
