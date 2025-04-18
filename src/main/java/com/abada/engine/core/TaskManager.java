package com.abada.engine.core;

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
    public void createTask(String taskId, String taskName, String processInstanceId,
                           String assignee, List<String> candidateUsers, List<String> candidateGroups) {
        TaskInstance task = new TaskInstance(taskId, taskName, processInstanceId, assignee, candidateUsers, candidateGroups);
        taskStore.put(taskId, task);
    }

    /**
     * Retrieve all tasks visible to a specific user (claimed or claimable).
     */
    public List<TaskInstance> getVisibleTasksForUser(String username, List<String> userGroups) {
        return taskStore.values().stream()
                .filter(task ->
                        username.equals(task.getAssignee()) ||
                                (task.getAssignee() == null && (
                                        task.getCandidateUsers().contains(username) ||
                                                userGroups.stream().anyMatch(group -> task.getCandidateGroups().contains(group))
                                ))
                ).collect(Collectors.toList());
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
    public boolean claimTask(String taskId, String username, List<String> userGroups) {
        TaskInstance task = taskStore.get(taskId);
        if (task != null && task.getAssignee() == null && (
                task.getCandidateUsers().contains(username) ||
                        userGroups.stream().anyMatch(group -> task.getCandidateGroups().contains(group))
        )) {
            TaskInstance claimed = new TaskInstance(
                    task.getTaskId(),
                    task.getTaskName(),
                    task.getProcessInstanceId(),
                    username,
                    task.getCandidateUsers(),
                    task.getCandidateGroups()
            );
            taskStore.put(taskId, claimed);
            return true;
        }
        return false;
    }
}
