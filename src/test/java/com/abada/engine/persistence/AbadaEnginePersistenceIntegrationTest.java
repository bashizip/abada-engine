package com.abada.engine.persistence;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.StateReloadService;
import com.abada.engine.core.TaskInstance;
import com.abada.engine.util.BpmnTestUtils;
import com.abada.engine.util.DatabaseTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class AbadaEnginePersistenceIntegrationTest {

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private StateReloadService stateReloadService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
     void setUpBeforeClass() throws Exception {
        DatabaseTestUtils.cleanDatabase(jdbcTemplate);
    }

    @Test
    void testProcessPersistenceAndReloading() {
        // 1. Load BPMN file from resources
        InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("claim-test.bpmn");
        abadaEngine.deploy(bpmnStream);

        // 2. Start the process
        String processId = abadaEngine.startProcess("claim-test");
        assertThat(processId).isNotNull();

        // 3. See the task created
        List<TaskInstance> tasks = abadaEngine.getVisibleTasks("alice", List.of());
        assertThat(tasks).isNotEmpty();
        TaskInstance taskBeforeCrash = tasks.get(0);

        // 4. Simulate crash
        abadaEngine.clearMemory();

        // 5. Reload from database
        stateReloadService.reloadStateAtStartup();

        // 6. See the task again after reload
        List<TaskInstance> tasksAfterReload = abadaEngine.getVisibleTasks("alice", List.of());
        assertThat(tasksAfterReload).isNotEmpty();
        TaskInstance reloadedTask = tasksAfterReload.get(0);
        assertThat(reloadedTask.getTaskDefinitionKey()).isEqualTo(taskBeforeCrash.getTaskDefinitionKey());

        // 7. Complete the task after reload
        boolean claimed = abadaEngine.claim(reloadedTask.getId(), "alice", List.of());
        assertThat(claimed).isTrue();

        boolean completed = abadaEngine.complete(reloadedTask.getId(), "alice", List.of());
        assertThat(completed).isTrue();

        // 8. Verify process completed
        ProcessInstance reloadedInstance = abadaEngine.getProcessInstanceById(processId);
        assertThat(reloadedInstance.isCompleted()).isTrue();
    }


}
