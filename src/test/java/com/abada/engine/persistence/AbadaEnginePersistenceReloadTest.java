package com.abada.engine.persistence;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.StateReloadService;
import com.abada.engine.core.TaskInstance;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;

import static com.abada.engine.util.DatabaseTestUtils.cleanDatabase;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("integration-persistence")
public class AbadaEnginePersistenceReloadTest {

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private StateReloadService stateReloadService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        cleanDatabase(jdbcTemplate); // or you can @Autowired JdbcTemplate separately
    }

    @Test
    void shouldRestoreEngineStateAfterMemoryWipe() throws Exception {
        // 1. Deploy and start
        InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("claim-test.bpmn");
        abadaEngine.deploy(bpmnStream);

        String processInstanceId = abadaEngine.startProcess("claim-test");
        assertNotNull(processInstanceId, "Process instance should be created");

        // 2. Validate task visibility before memory clear
        List<TaskInstance> visibleTasksBefore = abadaEngine.getVisibleTasks("alice", List.of());
        assertFalse(visibleTasksBefore.isEmpty(), "Alice should see tasks before memory clear");

        String taskIdBefore = visibleTasksBefore.get(0).getId();

        // 3. Simulate memory crash
        abadaEngine.clearMemory();

        // 4. Reload engine state from database
        stateReloadService.reloadStateAtStartup();

        // 5. Validate task visibility after reload
        List<TaskInstance> visibleTasksAfter = abadaEngine.getVisibleTasks("alice", List.of());
        assertFalse(visibleTasksAfter.isEmpty(), "Alice should still see tasks after reload");

        String taskIdAfter = visibleTasksAfter.get(0).getId();

        assertEquals(taskIdBefore, taskIdAfter, "Task ID should be the same before and after reload");

        // 6. Complete the task and verify process ends
        boolean claimed = abadaEngine.claim(taskIdAfter, "alice", List.of());
        assertTrue(claimed, "Alice should be able to claim task");

        boolean completed = abadaEngine.complete(taskIdAfter, "alice", List.of());
        assertTrue(completed, "Alice should be able to complete task");

        ProcessInstance reloadedInstance = abadaEngine.getProcessInstanceById(processInstanceId);
        assertTrue(reloadedInstance.isCompleted(), "Process should be completed after task completion");
    }
}
