package com.abada.engine.api;

import com.abada.engine.core.TaskInstance;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Arrays;

import static com.abada.engine.util.DatabaseTestUtils.cleanDatabase;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class EngineIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        System.out.println("cleanDatabase..");
        cleanDatabase(jdbcTemplate);
    }
    private void setupTestProcess() throws IOException {
        HttpHeaders deployHeaders = new HttpHeaders();
        deployHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource file = new ByteArrayResource(BpmnTestUtils.loadBpmnStream("test-process.bpmn")
                .readAllBytes()) {
            @Override
            public String getFilename() {
                return "sample.bpmn20.xml";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);

        HttpEntity<MultiValueMap<String, Object>> deployRequest = new HttpEntity<>(body, deployHeaders);
        restTemplate.postForEntity("/engine/deploy", deployRequest, String.class);

        HttpHeaders startHeaders = new HttpHeaders();
        startHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> startRequest = new HttpEntity<>("processId=claim-test.bpmn", startHeaders);
        restTemplate.postForEntity("/engine/start", startRequest, String.class);
    }

    @Test
    void shouldReturnTasksVisibleToAlice() throws IOException {
        setupTestProcess();

        ResponseEntity<TaskInstance[]> response = restTemplate.getForEntity(
                "/engine/tasks", TaskInstance[].class);

        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Response Body: " + Arrays.toString(response.getBody()));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length > 0);
    }

    @Test
    void shouldAllowAliceToClaimTask() throws IOException {
        setupTestProcess();

        ResponseEntity<TaskInstance[]> taskResponse = restTemplate.getForEntity(
                "/engine/tasks", TaskInstance[].class);

        assertNotNull(taskResponse.getBody(), "Response body is null");
        assertTrue(taskResponse.getBody().length > 0, "No tasks returned for alice");

        String taskId = taskResponse.getBody()[0].getId();

        ResponseEntity<String> claimResponse = restTemplate.postForEntity(
                "/engine/claim?taskId=" + taskId, null, String.class);

        assertEquals(HttpStatus.OK, claimResponse.getStatusCode());
        assertTrue(claimResponse.getBody().contains("Claimed"));
    }
}
