package com.abada.engine.core;

import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.dto.UserTaskPayload;
import com.abada.engine.parser.BpmnParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class InclusiveGatewayTest {

    private ParsedProcessDefinition definition;

    @BeforeEach
    void setUp() throws Exception {
        try (InputStream bpmnStream = getClass().getClassLoader().getResourceAsStream("bpmn/order-fulfillment.bpmn")) {
            assertNotNull(bpmnStream, "BPMN file not found");
            definition = new BpmnParser().parse(bpmnStream);
        }
    }

    @Test
    @DisplayName("Inclusive gateway should fork into multiple paths when conditions are met")
    void shouldFollowMultiplePathsWhenConditionsAreTrue() {
        // Given
        ProcessInstance pi = new ProcessInstance(definition);
        pi.setVariable("order", Map.of(
                "hasPhysicalItems", true,
                "hasDigitalItems", true,
                "hasDiscountCode", false
        ));

        // When
        List<UserTaskPayload> tasks = pi.advance();

        // Then
        assertEquals(2, tasks.size(), "Should have created two user tasks");
        List<String> taskIds = tasks.stream().map(UserTaskPayload::taskDefinitionKey).collect(Collectors.toList());
        assertTrue(taskIds.contains("Activity_Physical"), "Should contain task for physical goods");
        assertTrue(taskIds.contains("Activity_Digital"), "Should contain task for digital goods");

        // And the process instance should have two active tokens
        assertEquals(2, pi.getActiveTokens().size());
        assertTrue(pi.getActiveTokens().contains("Activity_Physical"));
        assertTrue(pi.getActiveTokens().contains("Activity_Digital"));
    }
}
