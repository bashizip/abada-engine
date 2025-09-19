package com.abada.engine.api;

import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ProcessApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl() {
        return "http://localhost:" + port + "/abada/api/v1/processes";
    }

    @BeforeEach
    void setup() throws IOException {
        // Deploy the process before each test to ensure it exists
        ByteArrayResource file = new ByteArrayResource(
                BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn").readAllBytes()) {
            @Override
            public String getFilename() {
                return "recipe-cook.bpmn";
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", file);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        restTemplate.postForEntity(baseUrl() + "/deploy", requestEntity, String.class);
    }

    @Test
    void shouldDeployProcess() throws Exception {
        // This test is now implicitly covered by the @BeforeEach setup,
        // but we can add an explicit assertion for clarity.
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("recipe-cook");
    }

    @Test
    void shouldListDeployedProcesses() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl(), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("id");
    }

    @Test
    void shouldStartProcess() {
        String processId = "recipe-cook";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>("processId=" + processId, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl() + "/start", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Started instance:");
    }

    @Test
    void shouldReturnNotFoundForMissingProcessId() {
        String fakeId = "nonexistent";
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl() + "/" + fakeId, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnProcessDetailsById() {
        String processId = "recipe-cook";
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl() + "/" + processId, Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKeys("id", "name", "bpmnXml");
    }
}
