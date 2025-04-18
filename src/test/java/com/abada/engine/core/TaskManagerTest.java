package com.abada.engine.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TaskManagerTest {

    private TaskManager taskManager;

    @BeforeEach
    void setup() {
        taskManager = new TaskManager();
    }

    @Test
    void shouldCreateAndRetrieveTasks() {
        taskManager.createTask("task1", "Approve Invoice", "proc-1", null,
                List.of("alice"), List.of("finance"));

        List<TaskInstance> tasks = taskManager.getVisibleTasksForUser("alice", List.of());
        assertEquals(1, tasks.size());

        TaskInstance task = tasks.get(0);
        assertEquals("task1", task.getTaskId());
        assertEquals("Approve Invoice", task.getTaskName());
        assertEquals("proc-1", task.getProcessInstanceId());
        assertNull(task.getAssignee());
        assertTrue(task.getCandidateUsers().contains("alice"));
    }

    @Test
    void shouldAllowClaimByCandidateUser() {
        taskManager.createTask("task2", "Review Report", "proc-2", null,
                List.of("bob"), List.of("hr"));

        boolean claimed = taskManager.claimTask("task2", "bob", List.of());
        assertTrue(claimed);

        TaskInstance claimedTask = taskManager.getTask("task2").orElseThrow();
        assertEquals("bob", claimedTask.getAssignee());
    }

    @Test
    void shouldAllowClaimByCandidateGroup() {
        taskManager.createTask("task3", "Submit Review", "proc-3", null,
                List.of(), List.of("qa"));

        // simulate that "carol" belongs to "qa" group
        List<String> userGroups = List.of("qa");
        List<TaskInstance> candidateTasks = taskManager.getVisibleTasksForUser("carol", userGroups);
        assertEquals(1, candidateTasks.size());

        boolean claimed = taskManager.claimTask("task3", "carol", userGroups);
        assertTrue(claimed);

        TaskInstance claimedTask = taskManager.getTask("task3").orElseThrow();
        assertEquals("carol", claimedTask.getAssignee());
    }


    @Test
    void shouldRejectClaimIfUnauthorized() {
        taskManager.createTask("task4", "Verify", "proc-4", null,
                List.of("alice"), List.of("devs"));

        boolean claimed = taskManager.claimTask("task4", "bob", List.of("hr"));
        assertFalse(claimed);
    }

    @Test
    void shouldRemoveTaskOnCompletion() {
        taskManager.createTask("task5", "Final Check", "proc-5", "john",
                List.of(), List.of());

        assertTrue(taskManager.getTask("task5").isPresent());
        taskManager.completeTask("task5");
        assertTrue(taskManager.getTask("task5").isEmpty());
    }
}
