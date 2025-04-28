package com.abada.engine.core;

import com.abada.engine.parser.BpmnParser;
import com.abada.engine.persistence.PersistenceService;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * Core engine responsible for managing process definitions and instances.
 */
@Component
public class AbadaEngine {

    private final PersistenceService persistenceService;
    private final BpmnParser parser;
    private final TaskManager taskManager;
    private final Map<String, ParsedProcessDefinition> processDefinitions = new HashMap<>();
    private final Map<String, ProcessInstance> instances = new HashMap<>();

    public AbadaEngine(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
        this.parser = new BpmnParser();
        this.taskManager = new TaskManager();
    }

    public void deploy(InputStream bpmnXml) {
        ParsedProcessDefinition definition = parser.parse(bpmnXml);
        processDefinitions.put(definition.getId(), definition);
    }

    public String startProcess(String processId) {
        ParsedProcessDefinition def = processDefinitions.get(processId);
        if (def == null) throw new IllegalArgumentException("Unknown process ID: " + processId);

        ProcessInstance instance = new ProcessInstance(def);
        instances.put(instance.getId(), instance);

        // Persist the new process instance
        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        String next = instance.advance();
        if (def.isUserTask(next)) {
            String name = def.getTaskName(next);
            String assignee = def.getTaskAssignee(next);
            List<String> users = def.getCandidateUsers(next);
            List<String> groups = def.getCandidateGroups(next);
            taskManager.createTask(next, name, instance.getId(), assignee, users, groups);

            // Persist the newly created task
            taskManager.getTaskByDefinitionKey(next, instance.getId())
                    .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));
        }

        return instance.getId();
    }

    public List<TaskInstance> getVisibleTasks(String user, List<String> groups) {
        return taskManager.getVisibleTasksForUser(user, groups);
    }

    public boolean claim(String taskId, String user, List<String> groups) {
        if (taskManager.claimTask(taskId, user, groups)) {
            taskManager.getTask(taskId)
                    .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));
            return true;
        }
        return false;
    }

    public boolean complete(String taskId, String user, List<String> groups) {
        if (taskManager.canComplete(taskId, user, groups)) {
            taskManager.completeTask(taskId);

            taskManager.getTask(taskId)
                    .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));

            String instanceId = taskManager.getTask(taskId)
                    .map(TaskInstance::getProcessInstanceId)
                    .orElseThrow();

            ProcessInstance instance = instances.get(instanceId);
            ParsedProcessDefinition def = instance.getDefinition();

            String next = instance.advance();

            // Persist updated process instance
            persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

            if (next != null && def.isUserTask(next)) {
                String name = def.getTaskName(next);
                String assignee = def.getTaskAssignee(next);
                List<String> users = def.getCandidateUsers(next);
                List<String> groupsList = def.getCandidateGroups(next);
                taskManager.createTask(next, name, instance.getId(), assignee, users, groupsList);

                // Persist the newly created task
                taskManager.getTaskByDefinitionKey(next, instance.getId())
                        .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));
            }
            return true;
        }
        return false;
    }


    private ProcessInstanceEntity convertToEntity(ProcessInstance instance) {
        ProcessInstanceEntity entity = new ProcessInstanceEntity();
        entity.setId(instance.getId());
        entity.setProcessDefinitionId(instance.getDefinition().getId());
        entity.setCurrentActivityId(instance.getCurrentActivityId());

        if (instance.isWaitingForUserTask()) {
            entity.setStatus(ProcessInstanceEntity.Status.WAITING_USER);
        } else if (instance.isCompleted()) {
            entity.setStatus(ProcessInstanceEntity.Status.COMPLETED);
        } else {
            entity.setStatus(ProcessInstanceEntity.Status.RUNNING);
        }

        return entity;
    }

    private TaskEntity convertToEntity(TaskInstance taskInstance) {
        TaskEntity entity = new TaskEntity();
        entity.setId(taskInstance.getId());
        entity.setProcessInstanceId(taskInstance.getProcessInstanceId());
        entity.setTaskDefinitionKey(taskInstance.getTaskDefinitionKey());
        entity.setName(taskInstance.getName());
        entity.setAssignee(taskInstance.getAssignee());

        if (taskInstance.isCompleted()) {
            entity.setStatus(TaskEntity.Status.COMPLETED);
        } else if (taskInstance.isClaimed()) {
            entity.setStatus(TaskEntity.Status.ASSIGNED);
        } else {
            entity.setStatus(TaskEntity.Status.CREATED);
        }

        return entity;
    }
}
