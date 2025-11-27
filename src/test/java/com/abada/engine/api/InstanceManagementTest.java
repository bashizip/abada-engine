package com.abada.engine.api;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.model.ProcessStatus;
import com.abada.engine.dto.CancelRequest;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class InstanceManagementTest {

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
        void shouldSuspendAndActivateProcess() {
                // 1. Suspend the process
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<SuspensionRequest> suspendRequest = new HttpEntity<>(new SuspensionRequest(true), headers);

                restTemplate.put("/v1/processes/api/v1/process-instances/" + instanceId + "/suspension",
                                suspendRequest);

                // Verify it is suspended
                ResponseEntity<ProcessInstanceDTO> response = restTemplate.getForEntity(
                                "/v1/processes/instances/" + instanceId,
                                ProcessInstanceDTO.class);
                assertTrue(response.getBody().suspended());

                // 2. Try to complete a task (should fail)
                // First get the task
                HttpEntity<Void> request = new HttpEntity<>(aliceHeaders);
                ResponseEntity<List<TaskDetailsDto>> taskResponse = restTemplate.exchange(
                                "/v1/tasks", HttpMethod.GET, request, new ParameterizedTypeReference<>() {
                                });
                String taskId = taskResponse.getBody().get(0).id();

                // Claim it
                restTemplate.postForEntity("/v1/tasks/claim?taskId=" + taskId + "&user=alice", null, Map.class);

                // Try to complete
                HttpHeaders completeHeaders = new HttpHeaders(aliceHeaders);
                completeHeaders.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> completeRequest = new HttpEntity<>(Map.of("goodOne", true),
                                completeHeaders);

                ResponseEntity<Map> completeResponse = restTemplate.postForEntity("/v1/tasks/complete?taskId=" + taskId,
                                completeRequest, Map.class);
                assertEquals(HttpStatus.BAD_REQUEST, completeResponse.getStatusCode()); // Or whatever exception
                                                                                        // handler returns

                // 3. Activate the process
                HttpEntity<SuspensionRequest> activateRequest = new HttpEntity<>(new SuspensionRequest(false), headers);
                restTemplate.put("/v1/processes/api/v1/process-instances/" + instanceId + "/suspension",
                                activateRequest);

                // Verify it is active
                response = restTemplate.getForEntity("/v1/processes/instances/" + instanceId, ProcessInstanceDTO.class);
                assertFalse(response.getBody().suspended());

                // 4. Complete the task (should succeed)
                completeResponse = restTemplate.postForEntity("/v1/tasks/complete?taskId=" + taskId, completeRequest,
                                Map.class);
                assertEquals(HttpStatus.OK, completeResponse.getStatusCode());
        }

        @Test
        void shouldCancelProcess() {
                // Cancel the process
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<CancelRequest> cancelRequest = new HttpEntity<>(new CancelRequest("Testing cancellation"),
                                headers);

                restTemplate.exchange("/v1/processes/api/v1/process-instances/" + instanceId, HttpMethod.DELETE,
                                cancelRequest,
                                Void.class);

                // Verify status
                ResponseEntity<ProcessInstanceDTO> response = restTemplate.getForEntity(
                                "/v1/processes/instances/" + instanceId,
                                ProcessInstanceDTO.class);
                assertEquals(ProcessStatus.CANCELLED, response.getBody().status());
                assertNotNull(response.getBody().endDate());
        }
}
