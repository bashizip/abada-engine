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
        List<TaskInstance> visibleTasks = new ArrayList<>();
        for (TaskInstance task : tasks.values()) {
            if (task.getAssignee() == null && isUserEligible(task, user, groups)) {
                visibleTasks.add(task);
            } else if (user.equals(task.getAssignee())) {
                visibleTasks.add(task);
            }
        }
        return visibleTasks;
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

    private boolean isUserEligible(TaskInstance task, String user, List<String> userGroups) {
        return (task.getCandidateUsers() != null && task.getCandidateUsers().contains(user)) ||
                (task.getCandidateGroups() != null && userGroups.stream().anyMatch(group -> task.getCandidateGroups().contains(group)));
    }

    public void addTask(TaskInstance taskInstance) {
        tasks.put(taskInstance.getId(), taskInstance);
    }

    public void clearTasks() {
        tasks.clear();
    }

}


