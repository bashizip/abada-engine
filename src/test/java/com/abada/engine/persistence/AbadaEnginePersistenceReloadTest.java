package com.abada.engine.persistence;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.StateReloadService;
import com.abada.engine.core.TaskInstance;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("integration-persistence")
public class AbadaEnginePersistenceReloadTest {

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private StateReloadService stateReloadService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private UserContextProvider context; // <-- inject a fake Context

    @BeforeEach
    void setupContext() {
        // Default user to "alice" before each test
        when(context.getUsername()).thenReturn("alice");
        when(context.getGroups()).thenReturn(List.of("customers"));
    }

    @Test
    void shouldRestoreEngineStateAfterMemoryWipe() throws Exception {
        // 1. Deploy and start
        InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn");
        abadaEngine.deploy(bpmnStream);

        String processInstanceId = abadaEngine.startProcess("recipe-cook");
        assertNotNull(processInstanceId, "Process instance should be created");

        // 2. Validate task visibility before memory clear
        List<TaskInstance> visibleTasksBefore = abadaEngine.getVisibleTasks("alice", List.of("customers"));
        assertFalse(visibleTasksBefore.isEmpty(), "Alice should see tasks before memory clear");

        String taskIdBefore = visibleTasksBefore.get(0).getId();

        // 3. Simulate memory crash
        abadaEngine.clearMemory();

        // 4. Reload engine state from database
        stateReloadService.reloadStateAtStartup();

        // 5. Validate task visibility after reload
        List<TaskInstance> visibleTasksAfter = abadaEngine.getVisibleTasks("alice", List.of("customers"));
        assertFalse(visibleTasksAfter.isEmpty(), "Alice should still see tasks after reload");

        String taskIdAfter = visibleTasksAfter.get(0).getId();

        assertEquals(taskIdBefore, taskIdAfter, "Task ID should be the same before and after reload");

        // 6. Complete the task and verify second task appears for Bob
        boolean claimed = abadaEngine.claim(taskIdAfter, "alice", List.of("customers"));
        assertTrue(claimed, "Alice should be able to claim task");

        boolean completed = abadaEngine.completeTask(taskIdAfter, "alice", List.of("customers"), Map.of("goodOne", true));
        assertTrue(completed, "Alice should be able to complete task");

        // 7. Switch context to Bob in 'cuistos' group
        when(context.getUsername()).thenReturn("bob");
        when(context.getGroups()).thenReturn(List.of("cuistos"));

        List<TaskInstance> tasksForBob = abadaEngine.getVisibleTasks("bob", List.of("cuistos"));
        assertFalse(tasksForBob.isEmpty(), "Bob should see the second task after Alice completes the first");

        String secondTaskId = tasksForBob.get(0).getId();

        boolean bobClaimed = abadaEngine.claim(secondTaskId, "bob", List.of("cuistos"));
        assertTrue(bobClaimed, "Bob should be able to claim the second task");

        boolean bobCompleted = abadaEngine.completeTask(secondTaskId, "bob", List.of("cuistos"), Collections.emptyMap());
        assertTrue(bobCompleted, "Bob should be able to complete the second task");

        ProcessInstance reloadedInstance = abadaEngine.getProcessInstanceById(processInstanceId);
        assertTrue(reloadedInstance.isCompleted(), "Process should be completed after Bob's task");
    }

    @AfterAll
    static void deleteTestDatabaseFile() {
        String basePath = "./build/test-db-abada-recovery";
        File mvDb = new File(basePath + ".mv.db");
        File traceDb = new File(basePath + ".trace.db");

        if (mvDb.exists()) {
            boolean deleted = mvDb.delete();
            System.out.println("Deleted mv.db: " + deleted);
        }

        if (traceDb.exists()) {
            boolean deleted = traceDb.delete();
            System.out.println("Deleted trace.db: " + deleted);
        }
    }
}
