package com.abada.engine.core;

import com.abada.engine.core.model.GatewayMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.SequenceFlow;
import com.abada.engine.core.model.TaskMeta;
import com.abada.engine.dto.UserTaskPayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Future‑proof tests for {@link ProcessInstance#advance()} that work with the
 * hand‑crafted ParsedProcessDefinition used in unit tests.
 *
 * Notes:
 * - We explicitly register outgoing flows via a reflective call to
 *   ParsedProcessDefinition.addOutgoing(sourceId, flow) when available.
 *   If your constructor already builds the outgoing index, this is a no‑op.
 * - Conditions use simple JS expressions (e.g., "x > 5"), which the engine
 *   evaluates via ConditionEvaluator.
 */
public class ProcessInstanceAdvanceTest {

    // ---------------------------------------------------------------------
    // Happy path: start -> gateway -> taskB when x > 5
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("advance(): follows conditional branch when expression is true")
    void advance_follows_true_branch() throws Exception {
        // user tasks
        TaskMeta taskB = new TaskMeta("taskB", "Task B", null, List.of(), List.of(), null, null, null, null, null);
        TaskMeta taskC = new TaskMeta("taskC", "Task C", null, List.of(), List.of(), null, null, null, null, null);

        // flows
        SequenceFlow f1 = new SequenceFlow("flow1", "start", "gateway1", null, null, false);
        SequenceFlow f2 = new SequenceFlow("flow2", "gateway1", "taskB", null, "x > 5", false);
        SequenceFlow f3 = new SequenceFlow("flow3", "gateway1", "taskC", null, null, true); // default

        // definition
        ParsedProcessDefinition def = new ParsedProcessDefinition(
                "proc1",
                "Test Process",
                "start",
                Map.of("taskB", taskB, "taskC", taskC),
                List.of(f1, f2, f3),
                Map.of("gateway1", new GatewayMeta("gateway1", GatewayMeta.Type.EXCLUSIVE, "flow3")),
                Collections.emptyMap(),
                "<xml/>"
        );
        registerOutgoing(def, f1, f2, f3);

        // runtime
        ProcessInstance pi = new ProcessInstance(def);
        pi.setCurrentActivityId("start");
        pi.setVariable("x", 10); // true for x > 5

        Optional<UserTaskPayload> out = pi.advance();
        assertTrue(out.isPresent(), "Expected to stop at a user task");
        assertEquals("taskB", out.get().taskDefinitionKey());
        assertEquals("Task B", out.get().name());
    }

    // ---------------------------------------------------------------------
    // Default path: start -> gateway -> taskC when condition is false
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("advance(): takes default flow when no condition matches")
    void advance_takes_default_when_false() throws Exception {
        TaskMeta taskB = new TaskMeta("taskB", "Task B", null, List.of(), List.of(), null, null, null, null, null);
        TaskMeta taskC = new TaskMeta("taskC", "Task C", null, List.of(), List.of(), null, null, null, null, null);

        SequenceFlow f1 = new SequenceFlow("flow1", "start", "gateway1", null, null, false);
        SequenceFlow f2 = new SequenceFlow("flow2", "gateway1", "taskB", null, "x > 5", false);
        SequenceFlow f3 = new SequenceFlow("flow3", "gateway1", "taskC", null, null, true);

        ParsedProcessDefinition def = new ParsedProcessDefinition(
                "proc1",
                "Test Process",
                "start",
                Map.of("taskB", taskB, "taskC", taskC),
                List.of(f1, f2, f3),
                Map.of("gateway1", new GatewayMeta("gateway1", GatewayMeta.Type.EXCLUSIVE, "flow3")),
                Collections.emptyMap(),
                "<xml/>"
        );
        registerOutgoing(def, f1, f2, f3);

        ProcessInstance pi = new ProcessInstance(def);
        pi.setCurrentActivityId("start");
        pi.setVariable("x", 3); // false for x > 5

        Optional<UserTaskPayload> out = pi.advance();
        assertTrue(out.isPresent(), "Expected to stop at a user task");
        assertEquals("taskC", out.get().taskDefinitionKey());
        assertEquals("Task C", out.get().name());
    }

    // ---------------------------------------------------------------------
    // Direct user task: start -> taskA (no gateway), ensures we stop on first user task
    // ---------------------------------------------------------------------
    @Test
    @DisplayName("advance(): stops at the first encountered user task")
    void advance_stops_on_first_user_task() throws Exception {
        TaskMeta taskA = new TaskMeta("taskA", "Task A", null, List.of(), List.of(), null, null, null, null, null);
        SequenceFlow f1 = new SequenceFlow("flow1", "start", "taskA", null, null, false);

        ParsedProcessDefinition def = new ParsedProcessDefinition(
                "proc1",
                "Test Process",
                "start",
                Map.of("taskA", taskA),
                List.of(f1),
                Collections.emptyMap(),
                Collections.emptyMap(),
                "<xml/>"
        );
        registerOutgoing(def, f1);

        ProcessInstance pi = new ProcessInstance(def);
        pi.setCurrentActivityId("start");

        Optional<UserTaskPayload> out = pi.advance();
        assertTrue(out.isPresent(), "Expected to stop at a user task");
        assertEquals("taskA", out.get().taskDefinitionKey());
        assertEquals("Task A", out.get().name());
        assertEquals("taskA", pi.getCurrentActivityId(), "Pointer should be set to the user task id");
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    /**
     * Register outgoing flows if the test ParsedProcessDefinition supports it.
     * If not present, this is a no‑op and we assume the constructor already
     * built the outgoing index.
     */
    private static void registerOutgoing(ParsedProcessDefinition def, SequenceFlow... flows) {
        try {
            Method m = ParsedProcessDefinition.class.getMethod("addOutgoing", String.class, SequenceFlow.class);
            for (SequenceFlow f : flows) {
                m.invoke(def, f.getSourceRef(), f);
            }
        } catch (NoSuchMethodException e) {
            // ignore — constructor likely builds the index from the List<SequenceFlow>
        } catch (Exception ex) {
            throw new RuntimeException("Failed to register outgoing flows via reflection", ex);
        }
    }
}
