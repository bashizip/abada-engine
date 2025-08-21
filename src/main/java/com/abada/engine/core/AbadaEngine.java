package com.abada.engine.core;

import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.dto.UserTaskPayload;
import com.abada.engine.parser.BpmnParser;
import com.abada.engine.persistence.PersistenceService;
import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
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

    public AbadaEngine(PersistenceService persistenceService, TaskManager taskManager) {
        this.persistenceService = persistenceService;
        this.parser = new BpmnParser();
        this.taskManager = taskManager;
    }

    public void deploy(InputStream bpmnXml) {
        ParsedProcessDefinition definition = parser.parse(bpmnXml);
        processDefinitions.put(definition.getId(), definition);
        saveProcessDefinition(definition);  // 🚨 SAVE real BPMN XML into DB
    }


    public List<ProcessDefinitionEntity> getDeployedProcesses() {
        return persistenceService.findAllProcessDefinitions();
    }

    public Optional<ProcessDefinitionEntity> getProcessDefinitionById(String id) {
        return Optional.ofNullable(persistenceService.findProcessDefinitionById(id));
    }

    public String startProcess(String processDefinitionId) {
        ParsedProcessDefinition definition = processDefinitions.get(processDefinitionId);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown process ID: " + processDefinitionId);
        }

        // 1. Create the new ProcessInstance
        ProcessInstance instance = new ProcessInstance(definition);
        instances.put(instance.getId(), instance);

        // 2. Persist initial process instance (still at start event)
        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        // 3. Move forward from start event to next node (should reach first userTask or end)
        Optional<UserTaskPayload> userTask = instance.advance();

        // 4. Persist updated process state after advance
        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        // 5. If the next node is a user task, create and persist the corresponding TaskInstance
        userTask.ifPresent(task -> {
            taskManager.createTask(
                    task.taskDefinitionKey(),
                    task.name(),
                    instance.getId(),
                    task.assignee(),
                    task.candidateUsers(),
                    task.candidateGroups()
            );
            // Persist the task into database
            taskManager.getTaskByDefinitionKey(task.taskDefinitionKey(), instance.getId())
                    .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));
        });

        Map<String, TaskInstance>  allTasks =  taskManager.getAllTasks();

        System.out.println("Process started, advancing...");
        System.out.println("Tasks loaded: " + allTasks);

       allTasks.forEach((k,t) ->
                System.out.println("→ " + t.getId() + ": " + t.getName()));

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

    public boolean complete(String taskId, String user, List<String> groups, Map<String, Object> variables) {
        System.out.println("Completing task " + taskId + " with variables: " + variables);
        if (!taskManager.canComplete(taskId, user, groups)) {
            return false;
        }

        // 1. Mark task as completed in memory
        taskManager.completeTask(taskId);

        // 2. Persist the updated task
        taskManager.getTask(taskId)
                .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));

        // 3. Get the associated process instance
        String processInstanceId = taskManager.getTask(taskId)
                .map(TaskInstance::getProcessInstanceId)
                .orElseThrow(() -> new IllegalStateException("Task has no associated process instance"));

        ProcessInstance instance = instances.get(processInstanceId);
        
        // Set variables
        if (variables != null) {
            variables.forEach(instance::setVariable);
        }

        ParsedProcessDefinition def = instance.getDefinition();

        // 4. Advance process to next activity (could be another user task or end)
        Optional<UserTaskPayload> nextTask = instance.advance();

        // 5. Persist updated process instance
        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        // 6. If next activity is a user task, create and persist it
        nextTask.ifPresent(task -> {
            taskManager.createTask(
                    task.taskDefinitionKey(),
                    task.name(),
                    processInstanceId,
                    task.assignee(),
                    task.candidateUsers(),
                    task.candidateGroups()
            );
            System.out.println("New task created: " + taskManager.getAllTasks());
            taskManager.getTaskByDefinitionKey(task.taskDefinitionKey(), processInstanceId)
                    .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));
        });

        return true;
    }

    public void rehydrateProcessInstance(ProcessInstanceEntity entity) {
        ParsedProcessDefinition def = processDefinitions.get(entity.getProcessDefinitionId());
        if (def == null) {
            throw new IllegalStateException("No deployed process definition found for ID: " + entity.getProcessDefinitionId());
        }

        ProcessInstance instance = new ProcessInstance(
                entity.getId(),
                def,
                entity.getCurrentActivityId()
        );
        instances.put(instance.getId(), instance);
    }

    public void rehydrateTaskInstance(TaskEntity entity) {
        TaskInstance task = new TaskInstance();
        task.setId(entity.getId());
        task.setProcessInstanceId(entity.getProcessInstanceId());
        task.setTaskDefinitionKey(entity.getTaskDefinitionKey());
        task.setName(entity.getName());
        task.setAssignee(entity.getAssignee());
        task.setCandidateUsers(entity.getCandidateUsers());
        task.setCandidateGroups(entity.getCandidateGroups());

        if (entity.getStatus() == TaskEntity.Status.COMPLETED) {
            task.setCompleted(true);
        }
        else if (entity.getStatus() == TaskEntity.Status.ASSIGNED) {
            task.setAssignee(entity.getAssignee());
        }

        taskManager.addTask(task);
    }




    private ProcessInstanceEntity convertToEntity(ProcessInstance instance) {
        ProcessInstanceEntity entity = new ProcessInstanceEntity();
        entity.setId(instance.getId());
        entity.setProcessDefinitionId(instance.getDefinition().getId());
        entity.setCurrentActivityId(instance.getCurrentActivityId());


        if (instance.isCompleted()) {
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

        entity.setCandidateUsers(new ArrayList<>(taskInstance.getCandidateUsers()));
        entity.setCandidateGroups(new ArrayList<>(taskInstance.getCandidateGroups()));

        if (taskInstance.isCompleted()) {
            entity.setStatus(TaskEntity.Status.COMPLETED);
        } else if (taskInstance.isClaimed()) {
            entity.setStatus(TaskEntity.Status.ASSIGNED);
        } else {
            entity.setStatus(TaskEntity.Status.CREATED);
        }

        return entity;
    }


    public void clearMemory() {
        instances.clear();
        taskManager.clearTasks();
    }
    public ProcessInstance getProcessInstanceById(String id) {
        return instances.get(id);
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public void saveProcessDefinition(ParsedProcessDefinition definition) {
        ProcessDefinitionEntity entity = new ProcessDefinitionEntity();
        entity.setId(definition.getId());
        entity.setName(definition.getName());
        entity.setBpmnXml(definition.getRawXml());
        persistenceService.saveProcessDefinition(entity);
    }

}
