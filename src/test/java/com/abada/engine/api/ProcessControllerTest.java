package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.dto.ProcessInstanceDTO;
import com.abada.engine.util.BpmnTestUtils;
import com.abada.engine.util.DatabaseTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consolidated integration tests for the ProcessController.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
public class ProcessControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    DatabaseTestHelper databaseTestHelper;

    private HttpHeaders authHeaders;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        databaseTestHelper.cleanup();

        authHeaders = new HttpHeaders();
        authHeaders.set("X-User", "test-user");
        authHeaders.set("X-Groups", "test-group");

        // Deploy the recipe-cook process before each test
        ByteArrayResource file = new ByteArrayResource(BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn").readAllBytes()) {
            @Override
            public String getFilename() {
                return "recipe-cook.bpmn";
            }
        };
        HttpHeaders deployHeaders = new HttpHeaders();
        deployHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        deployHeaders.addAll(authHeaders);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, deployHeaders);
        ResponseEntity<String> deployResponse = restTemplate.postForEntity("/v1/processes/deploy", requestEntity, String.class);
        assertThat(deployResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    /**
     * Verifies that the GET /v1/processes endpoint correctly lists all deployed process definitions.
     */
    @Test
    @DisplayName("GET /v1/processes should list deployed processes")
    void shouldListDeployedProcesses() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<List<Map<String, String>>> response = restTemplate.exchange(
                "/v1/processes", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get(0).get("id")).isEqualTo("recipe-cook");
    }

    /**
     * Verifies that the POST /v1/processes/start endpoint successfully starts a new process instance.
     */
    @Test
    @DisplayName("POST /v1/processes/start should start a process instance and return JSON")
    void shouldStartProcess() {
        HttpHeaders startHeaders = new HttpHeaders();
        startHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        startHeaders.addAll(authHeaders);
        HttpEntity<String> request = new HttpEntity<>("processId=recipe-cook", startHeaders);

        ResponseEntity<Map> response = restTemplate.postForEntity("/v1/processes/start", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("processInstanceId")).isNotNull();
    }

    /**
     * Verifies that the GET /v1/processes/{id} endpoint returns the correct details for a specific process definition.
     */
    @Test
    @DisplayName("GET /v1/processes/{id} should return process definition details")
    void shouldReturnProcessDetailsById() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<Map> response = restTemplate.exchange("/v1/processes/{id}", HttpMethod.GET, requestEntity, Map.class, "recipe-cook");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("id", "name", "bpmnXml");
        assertThat(response.getBody().get("id")).isEqualTo("recipe-cook");
    }

    /**
     * Verifies that the GET /v1/processes/instances endpoint returns a list of all active and completed process instances.
     */
    @Test
    @DisplayName("GET /v1/processes/instances should return all process instances")
    void shouldReturnAllProcessInstances() {
        abadaEngine.startProcess("recipe-cook");
        abadaEngine.startProcess("recipe-cook");

        HttpEntity<Void> requestEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<List<ProcessInstanceDTO>> response = restTemplate.exchange(
                "/v1/processes/instances", HttpMethod.GET, requestEntity, new ParameterizedTypeReference<>() {}
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull().hasSize(2);
    }

    /**
     * Verifies that the GET /v1/processes/{id} endpoint returns a 404 Not Found status for a process definition that does not exist.
     */
    @Test
    @DisplayName("GET /v1/processes/{id} should return 404 for a missing process definition")
    void shouldReturnNotFoundForMissingProcessId() {
        HttpEntity<Void> requestEntity = new HttpEntity<>(authHeaders);
        ResponseEntity<String> response = restTemplate.exchange("/v1/processes/{id}", HttpMethod.GET, requestEntity, String.class, "nonexistent");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
