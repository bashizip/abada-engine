package com.abada.engine.core;

import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.core.model.TaskStatus;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

        if (assignee != null && !assignee.isEmpty()) {
            task.setStatus(TaskStatus.CLAIMED);
        } else {
            task.setStatus(TaskStatus.AVAILABLE);
        }

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
        if (task == null || task.getStatus() != TaskStatus.AVAILABLE) {
            return false;
        }

        if (isUserEligible(task, user, userGroups)) {
            task.setAssignee(user);
            task.setStatus(TaskStatus.CLAIMED);
            return true;
        }
        return false;
    }

    public boolean canComplete(String taskId, String user, List<String> userGroups) {
        TaskInstance task = tasks.get(taskId);
        if (task == null || task.isCompleted()) {
            return false;
        }

        // A user can complete a task if they are the direct assignee
        if (user.equals(task.getAssignee())) {
            return true;
        }
        
        // Or if the task is available and they are an eligible candidate
        return task.getStatus() == TaskStatus.AVAILABLE && isUserEligible(task, user, userGroups);
    }

    public void completeTask(String taskId) {
        TaskInstance task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(TaskStatus.COMPLETED);
        }
    }

    public boolean failTask(String taskId) {
        TaskInstance task = tasks.get(taskId);
        if (task == null || task.isCompleted()) { // Can't fail a non-existent or completed task
            return false;
        }
        task.setStatus(TaskStatus.FAILED);
        return true;
    }

    public List<TaskInstance> getVisibleTasksForUser(String user, List<String> groups) {
        return getVisibleTasksForUser(user, groups, null);
    }

    public List<TaskInstance> getVisibleTasksForUser(String user, List<String> groups, TaskStatus status) {
        System.out.println("All tasks: " + tasks);
        Stream<TaskInstance> stream = tasks.values().stream()
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED)  // ✅ hide completed tasks
                .filter(task -> isUserEligible(task, user, groups));

        if (status != null) {
            stream = stream.filter(task -> task.getStatus() == status);
        }

        List<TaskInstance> result = stream.toList();
        System.out.println("Visible tasks for user " + user + " in groups " + groups + " with status " + status + ": " + result.toString());
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

    public List<TaskInstance> getTasksForProcessInstance(String processInstanceId) {
        return tasks.values().stream()
                .filter(t -> t.getProcessInstanceId().equals(processInstanceId) && t.getStatus() != TaskStatus.COMPLETED)
                .collect(Collectors.toList());
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
        // If unassigned, check candidate users and groups
        return task.getCandidateUsers().contains(user) ||
                (groups != null && groups.stream().anyMatch(task.getCandidateGroups()::contains));
    }


    public void addTask(TaskInstance taskInstance) {
        tasks.put(taskInstance.getId(), taskInstance);
    }

    public void clearTasks() {
        tasks.clear();
    }

}
