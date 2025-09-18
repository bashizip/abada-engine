package com.abada.engine.api;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.TaskManager;
import com.abada.engine.dto.MessageEventRequest;
import com.abada.engine.dto.SignalEventRequest;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AbadaEngineApplication.class)
@ActiveProfiles("test")
public class EventControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private TaskManager taskManager;

    @MockBean
    private UserContextProvider context;

    @BeforeEach
    void setUp() {
        abadaEngine.clearMemory();
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));
    }

    @Test
    @DisplayName("POST /api/v1/events/messages should correlate a message and resume a process")
    void shouldCorrelateMessageViaApi() throws Exception {
        // 1. Deploy the message event process
        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("message-event-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }

        // 2. Start a process and get it to the wait state
        ProcessInstance pi = abadaEngine.startProcess("MessageEventProcess");
        String correlationKey = UUID.randomUUID().toString();
        abadaEngine.completeTask(taskManager.getTasksForProcessInstance(pi.getId()).get(0).getId(), "test-user", List.of(), Map.of("correlationKey", correlationKey));

        // 3. Assert it's waiting for the message
        assertEquals("CatchEvent_OrderPaid", abadaEngine.getProcessInstanceById(pi.getId()).getActiveTokens().get(0));

        // 4. Send the message via the REST API
        MessageEventRequest request = new MessageEventRequest("OrderPaid", correlationKey, Map.of("paymentStatus", "CONFIRMED"));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/events/messages", request, Void.class);

        // 5. Assert the API call was successful and the process moved on
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        ProcessInstance resumedPi = abadaEngine.getProcessInstanceById(pi.getId());
        assertEquals("Task_FulfillOrder", resumedPi.getActiveTokens().get(0));
        assertEquals("CONFIRMED", resumedPi.getVariable("paymentStatus"));
    }

    @Test
    @DisplayName("POST /api/v1/events/signals should broadcast a signal and resume all waiting processes")
    void shouldBroadcastSignalViaApi() throws Exception {
        // 1. Deploy the signal event process
        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("signal-event-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }

        // 2. Start two processes and get them to the wait state
        ProcessInstance pi1 = abadaEngine.startProcess("SignalEventProcess");
        ProcessInstance pi2 = abadaEngine.startProcess("SignalEventProcess");
        abadaEngine.completeTask(taskManager.getTasksForProcessInstance(pi1.getId()).get(0).getId(), "test-user", List.of(), Map.of());
        abadaEngine.completeTask(taskManager.getTasksForProcessInstance(pi2.getId()).get(0).getId(), "test-user", List.of(), Map.of());

        // 3. Assert both are waiting for the signal
        assertEquals("CatchEvent_Signal", abadaEngine.getProcessInstanceById(pi1.getId()).getActiveTokens().get(0));
        assertEquals("CatchEvent_Signal", abadaEngine.getProcessInstanceById(pi2.getId()).getActiveTokens().get(0));

        // 4. Send the signal via the REST API
        SignalEventRequest request = new SignalEventRequest("SignalGo", Map.of("signalData", "BroadcastInfo"));
        ResponseEntity<Void> response = restTemplate.postForEntity("/api/v1/events/signals", request, Void.class);

        // 5. Assert the API call was successful and both processes moved on
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        ProcessInstance resumedPi1 = abadaEngine.getProcessInstanceById(pi1.getId());
        ProcessInstance resumedPi2 = abadaEngine.getProcessInstanceById(pi2.getId());

        assertEquals("FinalTask", resumedPi1.getActiveTokens().get(0));
        assertEquals("BroadcastInfo", resumedPi1.getVariable("signalData"));

        assertEquals("FinalTask", resumedPi2.getActiveTokens().get(0));
        assertEquals("BroadcastInfo", resumedPi2.getVariable("signalData"));
    }
}
