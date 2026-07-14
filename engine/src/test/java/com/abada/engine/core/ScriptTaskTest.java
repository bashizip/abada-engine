package com.abada.engine.core;

import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.util.BpmnTestUtils;
import com.abada.engine.util.DatabaseTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ScriptTaskTest {
    @Autowired private AbadaEngine engine;
    @Autowired private DatabaseTestHelper database;

    @BeforeEach
    void deploy() throws Exception {
        database.cleanup();
        try (InputStream bpmn = BpmnTestUtils.loadBpmnStream("script-task-test.bpmn")) {
            engine.deploy(bpmn);
        }
    }

    @Test
    void executesScriptBindingsAndCompletesAtNoneEndEvent() {
        ProcessInstance instance = engine.startProcess(
                "ScriptTaskProcess", "alice", Map.of("input", 21));
        assertThat(instance.getVariable("scriptResult")).isEqualTo(42.0);
        assertThat(instance.getActiveTokens()).containsExactly("review");

        TaskInstance review = engine.getTaskManager().getTasksForProcessInstance(instance.getId()).getFirst();
        engine.completeTask(review.getId(), "alice", List.of(), Map.of());
        assertThat(engine.getProcessInstanceById(instance.getId()).isCompleted()).isTrue();
    }
}
