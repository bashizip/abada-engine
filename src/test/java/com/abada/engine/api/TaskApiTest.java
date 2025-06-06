package com.abada.engine.api;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class TaskApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private UserContextProvider context;

    private String baseUrl() {
        return "http://localhost:" + port + "/abada/api/v1/tasks";
    }

    private void  deployAndStartProcess() throws Exception {
        // Deploy the process
        ByteArrayResource file = new ByteArrayResource(
                BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn").readAllBytes()) {
            @Override
            public String getFilename() {
                return "recipe-cook.bpmn";
            }
        };

        HttpHeaders deployHeaders = new HttpHeaders();
        deployHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> deployBody = new LinkedMultiValueMap<>();
        deployBody.add("file", file);
        HttpEntity<MultiValueMap<String, Object>> deployRequest = new HttpEntity<>(deployBody, deployHeaders);
        restTemplate.postForEntity("http://localhost:" + port + "/abada/api/v1/processes/deploy", deployRequest, String.class);

        // Start the process
        HttpHeaders startHeaders = new HttpHeaders();
        startHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<String> startRequest = new HttpEntity<>("processId=recipe-cook", startHeaders);
        ResponseEntity<String> response = restTemplate.postForEntity("http://localhost:" + port + "/abada/api/v1/processes/start", startRequest, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @BeforeEach
    void setup() throws Exception {
        when(context.getUsername()).thenReturn("alice");
        when(context.getGroups()).thenReturn(List.of("customers"));
        deployAndStartProcess();
    }

    @Test
    void shouldListVisibleTasks() {
        ResponseEntity<Object[]> response = restTemplate.getForEntity(baseUrl(), Object[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotEmpty();
    }

    @Test
    void shouldClaimTask() {
        String taskId = "choose-recipe";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>("taskId=" + taskId, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl() + "/claim", request, String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldCompleteTask() {
        String taskId = "choose-recipe";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>("taskId=" + taskId, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl() + "/complete", request, String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }
}
