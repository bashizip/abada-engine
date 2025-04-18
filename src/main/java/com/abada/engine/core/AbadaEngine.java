package com.abada.engine.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core Abada Engine responsible for managing process definitions and instances.
 */
public class AbadaEngine {

    private final Map<String, ProcessDefinition> processDefinitions = new ConcurrentHashMap<>();
    private final Map<String, ProcessInstance> processInstances = new ConcurrentHashMap<>();
    private final TaskManager taskManager = new TaskManager();

    /**
     * Deploy a parsed process definition into memory.
     */
    public void deploy(ProcessDefinition definition) {
        processDefinitions.put(definition.getId(), definition);
    }

    /**
     * Start a new instance of a process by its definition ID.
     */
    public String startProcess(String processId) {
        ProcessDefinition def = processDefinitions.get(processId);
        if (def == null) throw new IllegalArgumentException("Process not found: " + processId);

        ProcessInstance instance = new ProcessInstance(def);
        processInstances.put(instance.getId(), instance);

        // Advance the process to its first user task, if any
        String next = instance.advance();
        if (def.isUserTask(next)) {
            String taskName = def.getTaskName(next);
            String assignee = def.getTaskAssignee(next);
            List<String> candidateUsers = def.getCandidateUsers(next);
            List<String> candidateGroups = def.getCandidateGroups(next);

            taskManager.createTask(next, taskName, instance.getId(), assignee, candidateUsers, candidateGroups);
        }

        return instance.getId();
    }

    /**
     * Complete a task and continue the process flow.
     */
    public void completeTask(String taskId) {
        Optional<TaskInstance> optional = taskManager.getTask(taskId);
        if (optional.isEmpty()) throw new IllegalArgumentException("Task not found: " + taskId);

        TaskInstance task = optional.get();
        ProcessInstance instance = processInstances.get(task.getProcessInstanceId());
        if (instance == null) throw new IllegalStateException("Process instance not found");

        ProcessDefinition def = processDefinitions.get(instance.getId());

        taskManager.completeTask(taskId);
        String next = instance.advance();

        if (next != null && instance.isUserTask()) {
            String taskName = def.getTaskName(next);
            String assignee = def.getTaskAssignee(next);
            List<String> candidateUsers = def.getCandidateUsers(next);
            List<String> candidateGroups = def.getCandidateGroups(next);

            taskManager.createTask(next, taskName, instance.getId(), assignee, candidateUsers, candidateGroups);
        }
    }

    public List<TaskInstance> getVisibleTasks(String username, List<String> groups) {
        return taskManager.getVisibleTasksForUser(username, groups);
    }

    public List<TaskInstance> getCandidateTasks() {
        return taskManager.getCandidateTasks();
    }

    public boolean claimTask(String taskId, String username, List<String> groups) {
        return taskManager.claimTask(taskId, username, groups);
    }
}
