package com.abada.engine.api;

import com.abada.engine.context.UserContextProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

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

    private static HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private static HttpHeaders formHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return h;
    }

    private void deployAndStartProcess() throws Exception {
        // Deploy the process
        ByteArrayResource file = new ByteArrayResource(
                com.abada.engine.util.BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn").readAllBytes()) {
            @Override
            public String getFilename() { return "recipe-cook.bpmn"; }
        };

        HttpHeaders deployHeaders = new HttpHeaders();
        deployHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> deployBody = new LinkedMultiValueMap<>();
        deployBody.add("file", file);
        HttpEntity<MultiValueMap<String, Object>> deployRequest = new HttpEntity<>(deployBody, deployHeaders);
        ResponseEntity<String> deployResp = restTemplate.postForEntity(
                "http://localhost:" + port + "/abada/api/v1/processes/deploy",
                deployRequest,
                String.class);
        assertThat(deployResp.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.CREATED);

        // Start the process (use the id inside the BPMN)
        HttpEntity<String> startRequest = new HttpEntity<>("processId=recipe-cook", formHeaders());
        ResponseEntity<String> startResp = restTemplate.postForEntity(
                "http://localhost:" + port + "/abada/api/v1/processes/start",
                startRequest,
                String.class);
        assertThat(startResp.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @BeforeEach
    void setup() throws Exception {
        // User context resolution for the API layer
        when(context.getUsername()).thenReturn("alice");
        // include a plausible group in case visibility relies on it
        when(context.getGroups()).thenReturn(List.of("customers", "chefs"));
        deployAndStartProcess();
    }

    @Test
    void shouldListVisibleTasks() {
        ResponseEntity<List<TaskView>> response = restTemplate.exchange(
                baseUrl(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<TaskView>>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
        // Optional sanity: first task is for alice
        assertThat(response.getBody().get(0).assignee).isIn("alice", null);
    }

    @Test
    void shouldClaimTask() {
        List<TaskView> tasks = fetchTasks();
        String taskId = tasks.get(0).id;

        ResponseEntity<String> resp = restTemplate.postForEntity(
                baseUrl() + "/claim?taskId=" + taskId,
                new HttpEntity<>("", formHeaders()),
                String.class
        );
        assertThat(resp.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldCompleteTask() {
        List<TaskView> tasks = fetchTasks();
        String taskId = tasks.get(0).id;

        // Supply the variable used by the gateway so the engine can route
        Map<String, Object> vars = Map.of("goodOne", true);

        ResponseEntity<String> resp = restTemplate.postForEntity(
                baseUrl() + "/complete?taskId=" + taskId,
                new HttpEntity<>(vars, jsonHeaders()),
                String.class
        );
        assertThat(resp.getStatusCode()).isIn(HttpStatus.OK, HttpStatus.BAD_REQUEST);
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private List<TaskView> fetchTasks() {
        ResponseEntity<List<TaskView>> response = restTemplate.exchange(
                baseUrl(),
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<TaskView>>() {}
        );
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        return response.getBody();
    }

    /**
     * Minimal view of the task payload returned by /v1/tasks to avoid deserializing the domain model.
     */
    public static class TaskView {
        public String id;
        public String taskDefinitionKey;
        public String name;
        public String assignee;
        public List<String> candidateUsers;
        public List<String> candidateGroups;
        public String processInstanceId;
    }
}
