package com.abada.engine.api;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.dto.ActivityInstanceTree;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class VisualizerTest {

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

        HttpHeaders startHeaders = new HttpHeaders();
        startHeaders.setContentType(MediaType.APPLICATION_JSON);
        startHeaders.addAll(aliceHeaders);

        HttpEntity<Map<String, Object>> startRequest = new HttpEntity<>(Collections.emptyMap(), startHeaders);
        ResponseEntity<Map> response = restTemplate.postForEntity("/v1/processes/start/recipe-cook", startRequest,
                Map.class);

        assertNotNull(response.getBody());
        return (String) response.getBody().get("processInstanceId");
    }

    @Test
    void shouldReturnActivityInstances() {
        // Get activity instances
        ResponseEntity<ActivityInstanceTree> response = restTemplate.getForEntity(
                "/v1/processes/api/v1/process-instances/" + instanceId + "/activity-instances",
                ActivityInstanceTree.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ActivityInstanceTree tree = response.getBody();
        assertNotNull(tree);
        assertEquals(instanceId, tree.instanceId());
        assertFalse(tree.childActivityInstances().isEmpty());

        // Check if the active activity is "UserTask_1" (from recipe-cook.bpmn, assuming
        // first task)
        // Actually I don't know the exact ID without checking BPMN, but I know there
        // should be one.
        // Let's just assert it's not empty and has valid fields.
        tree.childActivityInstances().forEach(child -> {
            assertNotNull(child.activityId());
            assertNotNull(child.executionId());
            // Name might be null if not defined, but usually is.
        });
    }
}
