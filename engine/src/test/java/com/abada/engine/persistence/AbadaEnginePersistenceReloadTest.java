package com.abada.engine.persistence;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.StateReloadService;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.core.model.TaskStatus;
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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "abada.outbox.dispatcher.enabled=false")
@ActiveProfiles("integration-persistence")
public class AbadaEnginePersistenceReloadTest {

    @Autowired
    private AbadaEngine abadaEngine;
    @Autowired private com.abada.engine.util.DatabaseTestHelper databaseTestHelper;

    @Autowired
    private StateReloadService stateReloadService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private UserContextProvider context; // <-- inject a fake Context

    @BeforeEach
    void setupContext() {
        databaseTestHelper.cleanup();
        abadaEngine.clearMemory();
        // Default user to "alice" before each test
        when(context.getUsername()).thenReturn("alice");
        when(context.getGroups()).thenReturn(List.of("customers"));
    }

    @Test
    void shouldRestoreEngineStateAfterMemoryWipe() throws Exception {
        // 1. Deploy and start
        InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn");
        abadaEngine.deploy(bpmnStream);

        ProcessInstance processInstance= abadaEngine.startProcess("recipe-cook");
        assertNotNull(processInstance.getId(), "Process instance should be created");

        // 2. Validate task visibility before memory clear
        List<TaskInstance> visibleTasksBefore = abadaEngine.getTaskManager().getVisibleTasksForUser("alice", List.of("customers"));
        assertFalse(visibleTasksBefore.isEmpty(), "Alice should see tasks before memory clear");

        String taskIdBefore = visibleTasksBefore.get(0).getId();

        // 3. Simulate memory crash
        abadaEngine.clearMemory();

        // 4. Reload engine state from database
        stateReloadService.reloadStateAtStartup();

        // 5. Validate task visibility after reload
        List<TaskInstance> visibleTasksAfter = abadaEngine.getTaskManager().getVisibleTasksForUser("alice", List.of("customers"));
        assertFalse(visibleTasksAfter.isEmpty(), "Alice should still see tasks after reload");

        String taskIdAfter = visibleTasksAfter.get(0).getId();

        assertEquals(taskIdBefore, taskIdAfter, "Task ID should be the same before and after reload");

        // 6. Complete the task and verify second task appears for Bob
        abadaEngine.claim(taskIdAfter, "alice", List.of("customers"));
        TaskInstance claimedTask = abadaEngine.getTaskManager().getTask(taskIdAfter).orElseThrow();
        assertEquals(TaskStatus.CLAIMED, claimedTask.getStatus());

        abadaEngine.completeTask(taskIdAfter, "alice", List.of("customers"), Map.of("goodOne", true));
        TaskInstance completedTask = abadaEngine.getTaskManager().getTask(taskIdAfter).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, completedTask.getStatus());

        // 7. Switch context to Bob in 'cuistos' group
        when(context.getUsername()).thenReturn("bob");
        when(context.getGroups()).thenReturn(List.of("cuistos"));

        List<TaskInstance> tasksForBob = abadaEngine.getTaskManager().getVisibleTasksForUser("bob", List.of("cuistos"));
        assertFalse(tasksForBob.isEmpty(), "Bob should see the second task after Alice completes the first");

        String secondTaskId = tasksForBob.get(0).getId();

        abadaEngine.claim(secondTaskId, "bob", List.of("cuistos"));
        TaskInstance bobClaimedTask = abadaEngine.getTaskManager().getTask(secondTaskId).orElseThrow();
        assertEquals(TaskStatus.CLAIMED, bobClaimedTask.getStatus());

        abadaEngine.completeTask(secondTaskId, "bob", List.of("cuistos"), Collections.emptyMap());
        TaskInstance bobCompletedTask = abadaEngine.getTaskManager().getTask(secondTaskId).orElseThrow();
        assertEquals(TaskStatus.COMPLETED, bobCompletedTask.getStatus());

        ProcessInstance reloadedInstance = abadaEngine.getProcessInstanceById(processInstance.getId());
        assertTrue(reloadedInstance.isCompleted(), "Process should be isCompleted after Bob's task");
    }

    @Test
    void deploymentPersistsCanonicalCompilerAndCompatibilityMetadata() {
        var deployed = abadaEngine.deploy(BpmnTestUtils.loadBpmnStream("recipe-cook.bpmn"));

        assertEquals("canonical-1", deployed.getDefinitionFormatVersion());
        assertEquals("1", deployed.getCompilerVersion());
        assertTrue(deployed.getCompatibilityProfiles().contains("standard-bpmn-2.0"));
        assertTrue(deployed.getCompatibilityProfiles().contains("camunda-7"));
        assertTrue(deployed.getDetectedNamespaces().contains("http://camunda.org/schema/1.0/bpmn"));
        assertTrue(deployed.getCompatibilityReport().contains("canonical process model"));
    }

    @Test
    void allAssignmentDialectsPersistEquivalentCandidateGroups() throws Exception {
        for (String fixture : List.of("standard-assignment.bpmn", "abada-native-assignment.bpmn",
                "camunda-7-assignment.bpmn")) {
            try (var input = Files.newInputStream(Path.of("../examples/bpmn", fixture))) {
                var deployment = abadaEngine.deploy(input);
                abadaEngine.startProcess(deployment.getProcessKey());
            }
        }

        var tasks = abadaEngine.getTaskManager().getVisibleTasksForUser("reviewer", List.of("finance"));
        assertEquals(3, tasks.size());
        tasks.forEach(task -> assertEquals(List.of("finance"), task.getCandidateGroups()));
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
