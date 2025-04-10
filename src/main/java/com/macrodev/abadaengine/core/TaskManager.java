package com.macrodev.abadaengine.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory task service to register, query, and complete user tasks.
 */
public class TaskManager {

    private final Map<String, TaskInstance> taskStore = new ConcurrentHashMap<>();

    /**
     * Create a new task and store it in memory.
     */
    public void createTask(String taskId, String taskName, String processInstanceId, String assignee) {
        TaskInstance task = new TaskInstance(taskId, taskName, processInstanceId, assignee);
        taskStore.put(taskId, task);
    }

    /**
     * Retrieve all tasks assigned to a specific user.
     */
    public List<TaskInstance> getTasksForUser(String username) {
        return taskStore.values().stream()
                .filter(task -> username.equals(task.getAssignee()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve all unassigned tasks (available to claim).
     */
    public List<TaskInstance> getCandidateTasks() {
        return taskStore.values().stream()
                .filter(task -> task.getAssignee() == null)
                .collect(Collectors.toList());
    }

    /**
     * Complete a task by ID (removes it from the task list).
     */
    public void completeTask(String taskId) {
        taskStore.remove(taskId);
    }

    /**
     * Get a specific task by ID.
     */
    public Optional<TaskInstance> getTask(String taskId) {
        return Optional.ofNullable(taskStore.get(taskId));
    }

    /**
     * Assign a task to a user (claim).
     */
    public boolean claimTask(String taskId, String username) {
        TaskInstance task = taskStore.get(taskId);
        if (task != null && task.getAssignee() == null) {
            TaskInstance claimed = new TaskInstance(
                    task.getTaskId(), task.getTaskName(), task.getProcessInstanceId(), username);
            taskStore.put(taskId, claimed);
            return true;
        }
        return false;
    }
}

