package com.abada.engine.core;

import java.util.*;

public class TaskManager {

    private final Map<String, TaskInstance> tasks = new HashMap<>();

    public void createTask(String taskDefinitionKey, String name, String processInstanceId,
                           String assignee, List<String> candidateUsers, List<String> candidateGroups) {

        TaskInstance task = new TaskInstance();
        task.setId(UUID.randomUUID().toString());
        task.setTaskDefinitionKey(taskDefinitionKey);
        task.setName(name);
        task.setProcessInstanceId(processInstanceId);
        task.setAssignee(assignee);

        if (candidateUsers != null) {
            task.getCandidateUsers().addAll(candidateUsers);
        }
        if (candidateGroups != null) {
            task.getCandidateGroups().addAll(candidateGroups);
        }

        tasks.put(task.getId(), task);
    }


    public Map<String, TaskInstance> getAllTasks() {
        return tasks;
    }

    public boolean claimTask(String taskId, String user, List<String> userGroups) {
        TaskInstance task = tasks.get(taskId);
        if (task == null || task.getAssignee() != null) {
            return false;
        }

        if (isUserEligible(task, user, userGroups)) {
            task.setAssignee(user);
            return true;
        }
        return false;
    }

    public boolean canComplete(String taskId, String user, List<String> userGroups) {
        TaskInstance task = tasks.get(taskId);
        if (task == null) {
            return false;
        }

        return user.equals(task.getAssignee()) || isUserEligible(task, user, userGroups);
    }

    public void completeTask(String taskId) {
        TaskInstance task = tasks.get(taskId);
        if (task != null) {
            task.setCompleted(true);
        }
    }


    public List<TaskInstance> getVisibleTasksForUser(String user, List<String> groups) {
        return tasks.values().stream()
                .filter(task -> !task.isCompleted())  // ✅ hide completed tasks
                .filter(task -> isUserEligible(task, user, groups))
                .toList();
    }

    public Optional<TaskInstance> getTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public Optional<TaskInstance> getTaskByDefinitionKey(String taskDefinitionKey, String processInstanceId) {
        return tasks.values().stream()
                .filter(task -> taskDefinitionKey.equals(task.getTaskDefinitionKey()) &&
                        processInstanceId.equals(task.getProcessInstanceId()))
                .findFirst();
    }

    private boolean isUserEligible(TaskInstance task, String user, List<String> groups) {
        if (task.getAssignee() != null) {
            return task.getAssignee().equals(user); // direct assignee match
        }
        // Treat null inputs as empty collections to prevent NullPointerExceptions
        List<String> userGroups = groups != null ? groups : List.of();
        return task.getCandidateUsers().contains(user) ||
                userGroups.stream().anyMatch(task.getCandidateGroups()::contains);
    }


    public void addTask(TaskInstance taskInstance) {
        tasks.put(taskInstance.getId(), taskInstance);
    }

    public void clearTasks() {
        tasks.clear();
    }

}


