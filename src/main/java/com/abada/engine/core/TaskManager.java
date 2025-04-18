package com.abada.engine.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages user tasks including visibility, claiming, and completion.
 */
public class TaskManager {

    private final Map<String, TaskInstance> tasks = new ConcurrentHashMap<>();

    public void createTask(String taskId, String taskName, String processInstanceId,
                           String assignee, List<String> candidateUsers, List<String> candidateGroups) {
        TaskInstance task = new TaskInstance(taskId, taskName, processInstanceId,
                assignee, candidateUsers, candidateGroups);
        tasks.put(taskId, task);
    }

    public Optional<TaskInstance> getTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public List<TaskInstance> getVisibleTasksForUser(String username, List<String> groups) {
        List<TaskInstance> result = new ArrayList<>();
        for (TaskInstance task : tasks.values()) {
            if (task.getAssignee() != null && task.getAssignee().equals(username)) {
                result.add(task);
            } else if (task.getCandidateUsers().contains(username)) {
                result.add(task);
            } else if (!Collections.disjoint(task.getCandidateGroups(), groups)) {
                result.add(task);
            }
        }
        return result;
    }

    public boolean claimTask(String taskId, String username, List<String> groups) {
        TaskInstance task = tasks.get(taskId);
        if (task == null || task.getAssignee() != null) return false;

        boolean canClaim = task.getCandidateUsers().contains(username)
                || !Collections.disjoint(task.getCandidateGroups(), groups);
        if (canClaim) {
            task.setAssignee(username);
            return true;
        }
        return false;
    }

    public boolean canComplete(String taskId, String username, List<String> groups) {
        TaskInstance task = tasks.get(taskId);
        if (task == null) return false;

        return username.equals(task.getAssignee()) || task.getCandidateUsers().contains(username)
                || !Collections.disjoint(task.getCandidateGroups(), groups);
    }

    public void completeTask(String taskId) {
        tasks.remove(taskId);
    }
}
