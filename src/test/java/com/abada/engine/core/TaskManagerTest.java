package com.abada.engine.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskManagerTest {

    @Test
    void testCreateAndRetrieveTask() {
        TaskManager taskManager = new TaskManager();
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask(
                "approveTask", "Approve Request", processInstanceId,
                null,
                List.of("user1"),
                List.of("group1")
        );

        List<TaskInstance> visibleTasks = taskManager.getVisibleTasksForUser("user1", List.of("group1"));
        assertThat(visibleTasks).hasSize(1);

        TaskInstance task = visibleTasks.get(0);
        assertThat(task.getTaskDefinitionKey()).isEqualTo("approveTask");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstanceId);
        assertThat(task.getAssignee()).isNull();
        assertThat(task.isCompleted()).isFalse();
    }

    @Test
    void testClaimTask() {
        TaskManager taskManager = new TaskManager();
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask(
                "reviewTask", "Review Document", processInstanceId,
                null,
                List.of("user2"),
                List.of("group2")
        );

        List<TaskInstance> visible = taskManager.getVisibleTasksForUser("user2", List.of("group2"));
        assertThat(visible).hasSize(1);

        TaskInstance task = visible.get(0);

        boolean claimed = taskManager.claimTask(task.getId(), "user2", List.of("group2"));

        assertThat(claimed).isTrue();
        assertThat(task.getAssignee()).isEqualTo("user2");
        assertThat(task.isCompleted()).isFalse();
    }

    @Test
    void testCompleteTask() {
        TaskManager taskManager = new TaskManager();
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask(
                "signOffTask", "Final Sign Off", processInstanceId,
                "user3", // already assigned
                null,
                null
        );


        TaskInstance task = taskManager.getTaskByDefinitionKey("signOffTask", processInstanceId).orElseThrow();

        assertThat(task.getAssignee()).isEqualTo("user3");

        boolean canComplete = taskManager.canComplete(task.getId(), "user3", List.of("group3"));
        assertThat(canComplete).isTrue();
        taskManager.completeTask(task.getId());
        assertThat(task.isCompleted()).isTrue();

        // Once completed, it should no longer be visible
        List<TaskInstance> postCompleteVisible = taskManager.getVisibleTasksForUser("user3", List.of("group3"));
        assertThat(postCompleteVisible).isEmpty();
    }

    @Test
    void testGetTaskByDefinitionKey() {
        TaskManager taskManager = new TaskManager();
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask(
                "validateInvoice", "Validate Invoice", processInstanceId,
                null,
                List.of("user4"),
                List.of("group4")
        );

        Optional<TaskInstance> retrieved = taskManager.getTaskByDefinitionKey("validateInvoice", processInstanceId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Validate Invoice");
        assertThat(retrieved.get().isCompleted()).isFalse();
    }

    @Test
    void testVisibleTasksHandleNullGroupsAndCandidates() {
        TaskManager taskManager = new TaskManager();
        String processInstanceId = UUID.randomUUID().toString();

        // Simulate a task loaded from persistence with null candidate lists
        TaskInstance task = new TaskInstance();
        task.setId(UUID.randomUUID().toString());
        task.setProcessInstanceId(processInstanceId);
        task.setTaskDefinitionKey("t1");
        task.setCandidateUsers(null);
        task.setCandidateGroups(null);
        taskManager.addTask(task);

        // Should not throw when groups parameter is null
        List<TaskInstance> visible = taskManager.getVisibleTasksForUser("user", null);
        assertThat(visible).isEmpty();
    }
}
