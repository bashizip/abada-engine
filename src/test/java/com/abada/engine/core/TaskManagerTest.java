package com.abada.engine.core;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TaskManagerTest {

    @Test
    void testCreateAndRetrieveTask() {
        TaskManager taskManager = new TaskManager();
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask("approveTask", "Approve Request", processInstanceId, null, Arrays.asList("user1"), Arrays.asList("group1"));

        List<TaskInstance> visibleTasks = taskManager.getVisibleTasksForUser("user1", Arrays.asList("group1"));
        assertThat(visibleTasks).hasSize(1);

        TaskInstance task = visibleTasks.get(0);
        assertThat(task.getTaskDefinitionKey()).isEqualTo("approveTask");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstanceId);
        assertThat(task.getAssignee()).isNull();
    }

    @Test
    void testClaimTask() {
        TaskManager taskManager = new TaskManager();
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask("reviewTask", "Review Document", processInstanceId, null, Arrays.asList("user2"), Arrays.asList("group2"));

        TaskInstance task = taskManager.getVisibleTasksForUser("user2", Arrays.asList("group2")).get(0);
        boolean claimed = taskManager.claimTask(task.getId(), "user2", Arrays.asList("group2"));

        assertThat(claimed).isTrue();
        assertThat(task.getAssignee()).isEqualTo("user2");
    }

    @Test
    void testCompleteTask() {
        TaskManager taskManager = new TaskManager();
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask("reviewTask", "Review Document", processInstanceId, "user3", null, null);

        TaskInstance task = taskManager.getVisibleTasksForUser("user3", Arrays.asList("group3")).get(0);
        boolean canComplete = taskManager.canComplete(task.getId(), "user3", Arrays.asList("group3"));

        assertThat(canComplete).isTrue();

        taskManager.completeTask(task.getId());
        assertThat(task.isCompleted()).isTrue();
    }

    @Test
    void testGetTaskByDefinitionKey() {
        TaskManager taskManager = new TaskManager();
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask("validateInvoice", "Validate Invoice", processInstanceId, null, Arrays.asList("user4"), Arrays.asList("group4"));

        Optional<TaskInstance> retrievedTask = taskManager.getTaskByDefinitionKey("validateInvoice", processInstanceId);
        assertThat(retrievedTask).isPresent();
        assertThat(retrievedTask.get().getName()).isEqualTo("Validate Invoice");
    }
}
