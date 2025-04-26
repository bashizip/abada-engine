package com.abada.engine.core;

import com.abada.engine.parser.BpmnParser;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * Core engine responsible for managing process definitions and instances.
 */
@Component
public class AbadaEngine {

    private final BpmnParser parser = new BpmnParser();
    private final Map<String, ParsedProcessDefinition> processDefinitions = new HashMap<>();
    private final Map<String, ProcessInstance> instances = new HashMap<>();
    private final TaskManager taskManager = new TaskManager();

    public void deploy(InputStream bpmnXml) {
        ParsedProcessDefinition definition = parser.parse(bpmnXml);
        processDefinitions.put(definition.getId(), definition);
    }

    public String startProcess(String processId) {
        ParsedProcessDefinition def = processDefinitions.get(processId);
        if (def == null) throw new IllegalArgumentException("Unknown process ID: " + processId);

        ProcessInstance instance = new ProcessInstance(def);
        instances.put(instance.getId(), instance);

        String next = instance.advance();
        if (def.isUserTask(next)) {
            String name = def.getTaskName(next);
            String assignee = def.getTaskAssignee(next);
            List<String> users = def.getCandidateUsers(next);
            List<String> groups = def.getCandidateGroups(next);
            taskManager.createTask(next, name, instance.getId(), assignee, users, groups);
        }

        return instance.getId();
    }

    public List<TaskInstance> getVisibleTasks(String user, List<String> groups) {
        return taskManager.getVisibleTasksForUser(user, groups);
    }

    public boolean claim(String taskId, String user, List<String> groups) {
        return taskManager.claimTask(taskId, user, groups);
    }

    public boolean complete(String taskId, String user, List<String> groups) {
        if (taskManager.canComplete(taskId, user, groups)) {
            taskManager.completeTask(taskId);

            String instanceId = taskManager.getTask(taskId)
                    .map(TaskInstance::getProcessInstanceId)
                    .orElseThrow();

            ProcessInstance instance = instances.get(instanceId);
            ParsedProcessDefinition def = instance.getDefinition();

            String next = instance.advance();
            if (next != null && def.isUserTask(next)) {
                String name = def.getTaskName(next);
                String assignee = def.getTaskAssignee(next);
                List<String> users = def.getCandidateUsers(next);
                List<String> groupsList = def.getCandidateGroups(next);
                taskManager.createTask(next, name, instance.getId(), assignee, users, groupsList);
            }
            return true;
        }
        return false;
    }
}
