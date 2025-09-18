package com.abada.engine.core;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.persistence.repository.JobRepository;
import com.abada.engine.util.BpmnTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
public class TimerEventTest {

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobRepository jobRepository;

    @MockBean
    private UserContextProvider context;

    @BeforeEach
    void setUp() throws Exception {
        abadaEngine.clearMemory();
        jobRepository.deleteAll(); // Clear any previous jobs
        when(context.getUsername()).thenReturn("test-user");
        when(context.getGroups()).thenReturn(List.of("test-group"));

        try (InputStream bpmnStream = BpmnTestUtils.loadBpmnStream("timer-event-test.bpmn")) {
            abadaEngine.deploy(bpmnStream);
        }
    }

    @Test
    @DisplayName("Process should wait for a timer event and be resumed by the JobScheduler")
    void shouldWaitForAndBeResumedByTimerEvent() throws InterruptedException {
        // 1. Start the process and complete the initial task
        ProcessInstance pi = abadaEngine.startProcess("TimerEventProcess");
        TaskInstance initialTask = taskManager.getTasksForProcessInstance(pi.getId()).get(0);
        abadaEngine.completeTask(initialTask.getId(), "test-user", List.of(), Map.of());

        // 2. Assert that the process is waiting at the timer event
        ProcessInstance waitingPi = abadaEngine.getProcessInstanceById(pi.getId());
        assertEquals(1, waitingPi.getActiveTokens().size());
        assertEquals("CatchEvent_Timer", waitingPi.getActiveTokens().get(0));

        // 3. Assert that a job was created in the database
        assertEquals(1, jobRepository.count(), "A job should have been scheduled");

        // 4. Wait for a moment to ensure the job is due, then manually trigger the scheduler
        Thread.sleep(1100); // Wait just over 1 second to ensure the job is due
        jobScheduler.executeDueJobs();

        // 5. Assert that the process has resumed and moved to the final task
        ProcessInstance resumedPi = abadaEngine.getProcessInstanceById(pi.getId());
        List<TaskInstance> finalTasks = taskManager.getTasksForProcessInstance(resumedPi.getId());
        assertEquals(1, finalTasks.size(), "Should have moved to the final task");
        assertEquals("FinalTask", finalTasks.get(0).getTaskDefinitionKey());

        // 6. Assert that the job has been deleted
        assertEquals(0, jobRepository.count(), "The job should be deleted after execution");

        // 7. Complete the final task and assert the process is finished
        abadaEngine.completeTask(finalTasks.get(0).getId(), "test-user", List.of(), Map.of());
        assertTrue(abadaEngine.getProcessInstanceById(pi.getId()).isCompleted());
    }
}
