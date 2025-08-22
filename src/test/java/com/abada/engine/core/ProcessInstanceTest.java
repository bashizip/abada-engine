package com.abada.engine.core;

import com.abada.engine.core.model.GatewayMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.SequenceFlow;
import com.abada.engine.core.model.TaskMeta;
import com.abada.engine.dto.UserTaskPayloadDTO;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcessInstanceTest {

    @Test
    void shouldFollowConditionalPathBasedOnVariable() {
        // given: user tasks present in the model
        TaskMeta taskA = new TaskMeta("taskA", "Task A", null, List.of(), List.of(), null, null, null, null, null);
        TaskMeta taskB = new TaskMeta("taskB", "Task B", null, List.of(), List.of(), null, null, null, null, null);
        TaskMeta taskC = new TaskMeta("taskC", "Task C", null, List.of(), List.of(), null, null, null, null, null);

        // sequence flows (flow2 has a condition; flow3 is the default)
        SequenceFlow flow1 = new SequenceFlow("flow1", "start", "gateway1", null, null, false);
        SequenceFlow flow2 = new SequenceFlow("flow2", "gateway1", "taskB", null, "x > 5", false);
        SequenceFlow flow3 = new SequenceFlow("flow3", "gateway1", "taskC", null, null, true);
        SequenceFlow flow4 = new SequenceFlow("flow4", "taskB", "end", null, null, false);

        // parsed definition
        ParsedProcessDefinition definition = new ParsedProcessDefinition(
                "proc1",
                "Test Process",
                "start",
                Map.of(
                        "taskA", taskA,
                        "taskB", taskB,
                        "taskC", taskC
                ),
                List.of(flow1, flow2, flow3, flow4),
                Map.of("gateway1", new GatewayMeta("gateway1", GatewayMeta.Type.EXCLUSIVE, "flow3")),
                Collections.emptyMap(),
                "<xml/>"
        );

        ProcessInstance instance = new ProcessInstance(definition);
        instance.setCurrentActivityId("start");
        instance.setVariable("x", 10);

        // when
        Optional<UserTaskPayloadDTO> result = instance.advance();

        // then: we should reach Task B because x > 5 is true
        assertTrue(result.isPresent(), "Expected to stop at a user task");
        assertEquals("Task B", result.get().name());
        assertEquals("taskB", result.get().taskDefinitionKey());
    }
}
