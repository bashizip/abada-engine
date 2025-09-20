package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
public class EventControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AbadaEngine abadaEngine;

    private HttpHeaders httpHeaders;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        // Deploy the process once for all tests in this class
        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("event-based-process.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
        httpHeaders = new HttpHeaders();
        httpHeaders.set("X-User", "test-user");
        httpHeaders.set("X-Groups", "test-group");
    }

    @Test
    @DisplayName("Should trigger a process with a specific message event via API")
    void shouldTriggerProcessByMessageEvent() {
        String url = "/v1/events/message/event1";
        Map<String, Object> body = Map.of("variable1", "value1", "variable2", "value2");
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, httpHeaders);

        ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
