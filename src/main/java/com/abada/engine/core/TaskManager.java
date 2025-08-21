package com.abada.engine.core;

import com.abada.engine.core.model.TaskInstance;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TaskManager {

    private final Map<String, TaskInstance> tasks = new HashMap<>();

    public void createTask(String taskDefinitionKey, String name, String processInstanceId,
                           String assignee, List<String> candidateUsers, List<String> candidateGroups) {

        System.out.println("Creating task: " + name);
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
        System.out.println("All tasks: " + tasks);
        List<TaskInstance> result = tasks.values().stream()
                .filter(task -> !task.isCompleted())  // ✅ hide completed tasks
                .filter(task -> isUserEligible(task, user, groups))
                .toList();
        System.out.println("Visible tasks for user " + user + " in groups " + groups + ": " + result.toString());
        return result;
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
        System.out.println("Checking eligibility for task: " + task.getName());
        System.out.println("Task candidate users: " + task.getCandidateUsers());
        System.out.println("Task candidate groups: " + task.getCandidateGroups());
        System.out.println("User: " + user);
        System.out.println("User groups: " + groups);
        if (task.getAssignee() != null) {
            return task.getAssignee().equals(user); // direct assignee match
        }
        return task.getCandidateUsers().contains(user) ||
                groups.stream().anyMatch(task.getCandidateGroups()::contains);
    }


    public void addTask(TaskInstance taskInstance) {
        tasks.put(taskInstance.getId(), taskInstance);
    }

    public void clearTasks() {
        tasks.clear();
    }

}


