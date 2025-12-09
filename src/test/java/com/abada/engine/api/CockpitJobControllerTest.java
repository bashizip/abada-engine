package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.dto.ActiveJobDTO;
import com.abada.engine.dto.FetchAndLockRequest;
import com.abada.engine.persistence.repository.ExternalTaskRepository;
import com.abada.engine.persistence.repository.JobRepository;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
class CockpitJobControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private ExternalTaskRepository externalTaskRepository;

    @Autowired
    private JobRepository jobRepository;

    private HttpHeaders headers;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        externalTaskRepository.deleteAll();
        jobRepository.deleteAll();
        headers = new HttpHeaders();
        headers.set("X-User", "test-user");

        // Deploy necessary processes
        try (InputStream is = BpmnTestUtils.loadBpmnStream("external-task-test.bpmn")) {
            abadaEngine.deploy(is);
        }
        try (InputStream is = BpmnTestUtils.loadBpmnStream("timer-event-test.bpmn")) {
            abadaEngine.deploy(is);
        }
        try (InputStream is = BpmnTestUtils.loadBpmnStream("message-event-test.bpmn")) {
            abadaEngine.deploy(is);
        }
    }

    @Test
    @DisplayName("GET /v1/jobs/active should return all active jobs (external, timer, message)")
    void shouldListActiveJobs() {
        // 1. Start External Task Process and Lock it
        ProcessInstance extPi = abadaEngine.startProcess("ExternalTaskTestProcess");
        FetchAndLockRequest fetchRequest = new FetchAndLockRequest("worker-1", List.of("test-topic"), 10000L);
        restTemplate.postForEntity("/v1/external-tasks/fetch-and-lock", new HttpEntity<>(fetchRequest, headers),
                List.class);

        // 2. Start Timer Process
        ProcessInstance timerPi = abadaEngine.startProcess("TimerEventProcess");
        // Complete the first task to reach the timer
        String taskId = abadaEngine.getTaskManager().getTasksForProcessInstance(timerPi.getId()).get(0).getId();
        abadaEngine.completeTask(taskId, "test-user", List.of(), null);

        // 3. Start Message Process
        ProcessInstance msgPi = abadaEngine.startProcess("MessageEventProcess");
        // Complete the first task to reach the message catch event
        String msgTaskId = abadaEngine.getTaskManager().getTasksForProcessInstance(msgPi.getId()).get(0).getId();
        abadaEngine.completeTask(msgTaskId, "test-user", List.of(), null);

        // 4. Call Active Jobs Endpoint
        ResponseEntity<List<ActiveJobDTO>> response = restTemplate.exchange(
                "/v1/jobs/active",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {
                });

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        List<ActiveJobDTO> jobs = response.getBody();
        assertThat(jobs).isNotNull();

        // Verify External Task
        assertThat(jobs).anyMatch(j -> j.type().equals("EXTERNAL_TASK")
                && j.processInstanceId().equals(extPi.getId())
                && j.details().contains("test-topic"));

        // Verify Timer
        assertThat(jobs).anyMatch(j -> j.type().equals("TIMER")
                && j.processInstanceId().equals(timerPi.getId()));

        // Verify Message
        assertThat(jobs).anyMatch(j -> j.type().equals("MESSAGE")
                && j.processInstanceId().equals(msgPi.getId())
                && j.details().contains("Waiting for MESSAGE"));
    }
}
