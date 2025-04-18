package com.abada.engine.api;

import com.abada.engine.core.TaskInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EngineIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private final String sampleBpmn = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<definitions xmlns=\"http://www.omg.org/spec/BPMN/20100524/MODEL\"\n" +
            "             xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "             targetNamespace=\"http://abada/engine/test\">\n" +
            "  <process id=\"claim-test\" name=\"Claim Test\" isExecutable=\"true\">\n" +
            "    <startEvent id=\"start\"/>\n" +
            "    <sequenceFlow id=\"s1\" sourceRef=\"start\" targetRef=\"task1\"/>\n" +
            "    <userTask id=\"task1\" name=\"Do something\" candidateUsers=\"alice\"/>\n" +
            "    <sequenceFlow id=\"s2\" sourceRef=\"task1\" targetRef=\"end\"/>\n" +
            "    <endEvent id=\"end\"/>\n" +
            "  </process>\n" +
            "</definitions>";

    private void setupTestProcess() {
        HttpHeaders deployHeaders = new HttpHeaders();
        deployHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource file = new ByteArrayResource(sampleBpmn.getBytes(StandardCharsets.UTF_8)) {
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

        HttpEntity<String> startRequest = new HttpEntity<>("processId=claim-test", startHeaders);
        restTemplate.postForEntity("/engine/start", startRequest, String.class);
    }

    @Test
    void shouldReturnTasksVisibleToAlice() {
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
    void shouldAllowAliceToClaimTask() {
        setupTestProcess();

        ResponseEntity<TaskInstance[]> taskResponse = restTemplate.getForEntity(
                "/engine/tasks", TaskInstance[].class);

        assertNotNull(taskResponse.getBody(), "Response body is null");
        assertTrue(taskResponse.getBody().length > 0, "No tasks returned for alice");

        String taskId = taskResponse.getBody()[0].getTaskId();

        ResponseEntity<String> claimResponse = restTemplate.postForEntity(
                "/engine/claim?taskId=" + taskId, null, String.class);

        assertEquals(HttpStatus.OK, claimResponse.getStatusCode());
        assertTrue(claimResponse.getBody().contains("Claimed"));
    }
}
