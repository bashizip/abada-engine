package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.dto.ProcessInstanceDTO;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
public class ProcessControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AbadaEngine abadaEngine;

    @MockBean
    private UserContextProvider context;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));

        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("service-task-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    @Test
    @DisplayName("GET /api/v1/processes/instances should return all process instances")
    void shouldReturnAllProcessInstances() {
        // 1. Start two process instances
        abadaEngine.startProcess("ServiceTaskTestProcess");
        abadaEngine.startProcess("ServiceTaskTestProcess");

        // 2. Call the new endpoint to get all instances
        ResponseEntity<List<ProcessInstanceDTO>> response = restTemplate.exchange(
                "/v1/processes/instances",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        // 3. Assert the response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
    }
}
