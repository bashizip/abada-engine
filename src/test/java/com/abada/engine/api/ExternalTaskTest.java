package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.dto.ExternalTaskFailureDto;
import com.abada.engine.dto.FailedJobDTO;
import com.abada.engine.dto.FetchAndLockRequest;
import com.abada.engine.dto.LockedExternalTask;
import com.abada.engine.persistence.entity.ExternalTaskEntity;
import com.abada.engine.persistence.repository.ExternalTaskRepository;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
public class ExternalTaskTest {

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private AbadaEngine abadaEngine;

        @Autowired
        private ExternalTaskRepository externalTaskRepository;

        private HttpHeaders headers;

        @BeforeEach
        void setUp() throws Exception {
                abadaEngine.clearMemory();
                externalTaskRepository.deleteAll();
                headers = new HttpHeaders();
                headers.set("X-User", "test-user");
                headers.set("X-Groups", "test-group");

                try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("external-task-test.bpmn")) {
                        abadaEngine.deploy(bpmnStream);
                }
        }

        @Test
        @DisplayName("External worker should fetch, lock, and complete an external task")
        void shouldCreateAndCompleteExternalTaskViaApi() {
                // 1. Start the process
                ProcessInstance pi = abadaEngine.startProcess("ExternalTaskTestProcess");

                // 2. Assert that the process is waiting at the service task and a job was
                // created
                assertEquals("ServiceTask_External", pi.getActiveTokens().get(0));
                assertEquals(1, externalTaskRepository.count());

                // 3. Simulate a worker fetching and locking the task
                FetchAndLockRequest fetchRequest = new FetchAndLockRequest("worker-1", List.of("test-topic"), 10000L);
                HttpEntity<FetchAndLockRequest> requestEntity = new HttpEntity<>(fetchRequest, headers);
                ResponseEntity<List<LockedExternalTask>> lockResponse = restTemplate.exchange(
                                "/v1/external-tasks/fetch-and-lock",
                                HttpMethod.POST,
                                requestEntity,
                                new ParameterizedTypeReference<List<LockedExternalTask>>() {
                                });

                assertEquals(HttpStatus.OK, lockResponse.getStatusCode());
                List<LockedExternalTask> lockedTasks = lockResponse.getBody();
                assertNotNull(lockedTasks);
                assertEquals(1, lockedTasks.size());
                LockedExternalTask lockedTask = lockedTasks.get(0);
                assertEquals("test-topic", lockedTask.topicName());

                // 4. Simulate the worker completing the task
                Map<String, Object> outputVariables = Map.of("externalTaskResult", "SUCCESS");
                HttpEntity<Map<String, Object>> completeRequestEntity = new HttpEntity<>(outputVariables, headers);
                restTemplate.postForEntity("/v1/external-tasks/{id}/complete", completeRequestEntity, Void.class,
                                lockedTask.id());

                // 5. Assert that the process has resumed and moved to the final user task
                ProcessInstance resumedPi = abadaEngine.getProcessInstanceById(pi.getId());
                assertEquals("FinalTask", resumedPi.getActiveTokens().get(0));
                assertEquals("SUCCESS", resumedPi.getVariable("externalTaskResult"));

                // 6. Assert that the external task job has been deleted
                assertEquals(0, externalTaskRepository.count());
        }

        @Test
        @DisplayName("External worker should report failure and job should appear in failed jobs list")
        void shouldHandleExternalTaskFailure() {
                // 1. Start the process
                ProcessInstance pi = abadaEngine.startProcess("ExternalTaskTestProcess");

                // 2. Fetch and lock
                FetchAndLockRequest fetchRequest = new FetchAndLockRequest("worker-1", List.of("test-topic"), 10000L);
                HttpEntity<FetchAndLockRequest> requestEntity = new HttpEntity<>(fetchRequest, headers);
                ResponseEntity<List<LockedExternalTask>> lockResponse = restTemplate.exchange(
                                "/v1/external-tasks/fetch-and-lock",
                                HttpMethod.POST,
                                requestEntity,
                                new ParameterizedTypeReference<List<LockedExternalTask>>() {
                                });
                LockedExternalTask lockedTask = lockResponse.getBody().get(0);

                // 3. Report failure
                ExternalTaskFailureDto failureDto = new ExternalTaskFailureDto(
                                "worker-1",
                                "Something went wrong",
                                "Stack trace details...",
                                0,
                                1000L);
                HttpEntity<ExternalTaskFailureDto> failureRequestEntity = new HttpEntity<>(failureDto, headers);
                restTemplate.postForEntity("/v1/external-tasks/{id}/failure", failureRequestEntity, Void.class,
                                lockedTask.id());

                // 4. Verify task is FAILED in DB
                ExternalTaskEntity task = externalTaskRepository.findById(lockedTask.id()).orElseThrow();
                assertEquals(ExternalTaskEntity.Status.FAILED, task.getStatus());
                assertEquals("Something went wrong", task.getExceptionMessage());
                assertEquals(0, task.getRetries());

                // 5. Verify it appears in failed jobs list
                ResponseEntity<List<FailedJobDTO>> failedJobsResponse = restTemplate.exchange(
                                "/v1/jobs/failed",
                                HttpMethod.GET,
                                new HttpEntity<>(headers),
                                new ParameterizedTypeReference<List<FailedJobDTO>>() {
                                });

                List<FailedJobDTO> failedJobs = failedJobsResponse.getBody();
                assertNotNull(failedJobs);
                assertEquals(1, failedJobs.size());
                assertEquals(lockedTask.id(), failedJobs.get(0).jobId());
                assertEquals("Something went wrong", failedJobs.get(0).exceptionMessage());
        }
}
