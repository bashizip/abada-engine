package com.abada.engine.core;

import com.abada.engine.core.model.EventMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.ServiceTaskMeta;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.dto.UserTaskPayload;
import com.abada.engine.parser.BpmnParser;
import com.abada.engine.persistence.PersistenceService;
import com.abada.engine.persistence.entity.ExternalTaskEntity;
import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import com.abada.engine.persistence.repository.ExternalTaskRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class AbadaEngine {

    private static final Logger log = LoggerFactory.getLogger(AbadaEngine.class);

    private final PersistenceService persistenceService;
    private final BpmnParser parser;
    private final TaskManager taskManager;
    private final EventManager eventManager;
    private final JobScheduler jobScheduler;
    private final ExternalTaskRepository externalTaskRepository;
    private final ObjectMapper om;
    private final Map<String, ParsedProcessDefinition> processDefinitions = new HashMap<>();
    private final Map<String, ProcessInstance> instances = new HashMap<>();

    @Autowired
    public AbadaEngine(PersistenceService persistenceService, TaskManager taskManager, EventManager eventManager, @Lazy JobScheduler jobScheduler, ExternalTaskRepository externalTaskRepository, ObjectMapper om) {
        this.persistenceService = persistenceService;
        this.parser = new BpmnParser();
        this.taskManager = taskManager;
        this.eventManager = eventManager;
        this.jobScheduler = jobScheduler;
        this.externalTaskRepository = externalTaskRepository;
        this.om = om;
    }

    @PostConstruct
    public void setup() {
        eventManager.setAbadaEngine(this);
        jobScheduler.setAbadaEngine(this);
    }

    public void deploy(InputStream bpmnXml) {
        ParsedProcessDefinition definition = parser.parse(bpmnXml);
        processDefinitions.put(definition.getId(), definition);
        saveProcessDefinition(definition);
        log.info("Deployed process definition: {}", definition.getId());
    }


    public List<ProcessDefinitionEntity> getDeployedProcesses() {
        return persistenceService.findAllProcessDefinitions();
    }

    public Optional<ProcessDefinitionEntity> getProcessDefinitionById(String id) {
        return Optional.ofNullable(persistenceService.findProcessDefinitionById(id));
    }

    public ParsedProcessDefinition getParsedProcessDefinition(String processDefinitionId) {
        return processDefinitions.get(processDefinitionId);
    }

    public ProcessInstance startProcess(String processDefinitionId) {
        ParsedProcessDefinition definition = processDefinitions.get(processDefinitionId);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown process ID: " + processDefinitionId);
        }

        ProcessInstance instance = new ProcessInstance(definition);
        instances.put(instance.getId(), instance);
        log.info("Started process instance: {}", instance.getId());

        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        List<UserTaskPayload> userTasks = instance.advance();

        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        for (UserTaskPayload task : userTasks) {
            createAndPersistTask(task, instance.getId());
        }

        eventManager.registerWaitStates(instance);
        scheduleWaitingTimerEvents(instance);
        createExternalTaskJobs(instance);

        return instance;
    }

    public boolean claim(String taskId, String user, List<String> groups) {
        if (taskManager.claimTask(taskId, user, groups)) {
            taskManager.getTask(taskId)
                    .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));
            return true;
        }
        return false;
    }


    public boolean completeTask(String taskId, String user, List<String> groups, Map<String, Object> variables) {
        log.info("Completing task {} with variables: {}", taskId, variables);

        if (!taskManager.canComplete(taskId, user, groups)) {
            log.warn("User {} cannot complete task {}", user, taskId);
            return false;
        }

        TaskInstance currentTask = taskManager.getTask(taskId)
                .orElseThrow(() -> new IllegalStateException("Task not found: " + taskId));

        String processInstanceId = currentTask.getProcessInstanceId();
        ProcessInstance instance = instances.get(processInstanceId);
        if (instance == null) {
            throw new IllegalStateException("No process instance found for id=" + processInstanceId);
        }

        if (variables != null && !variables.isEmpty()) {
            instance.putAllVariables(variables);
        }

        taskManager.completeTask(taskId);
        persistenceService.saveTask(convertToEntity(currentTask));
        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        List<UserTaskPayload> nextTasks = instance.advance(currentTask.getTaskDefinitionKey());

        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        for (UserTaskPayload task : nextTasks) {
            createAndPersistTask(task, processInstanceId);
        }

        eventManager.registerWaitStates(instance);
        scheduleWaitingTimerEvents(instance);
        createExternalTaskJobs(instance);

        return true;
    }

    public void resumeFromEvent(String processInstanceId, String eventId, Map<String, Object> variables) {
        log.info("Resuming process instance {} from event {}", processInstanceId, eventId);
        ProcessInstance instance = instances.get(processInstanceId);
        if (instance == null) {
            throw new IllegalStateException("No process instance found for id=" + processInstanceId);
        }

        if (variables != null && !variables.isEmpty()) {
            instance.putAllVariables(variables);
        }

        List<UserTaskPayload> nextTasks = instance.advance(eventId);

        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        for (UserTaskPayload task : nextTasks) {
            createAndPersistTask(task, processInstanceId);
        }

        eventManager.registerWaitStates(instance);
        scheduleWaitingTimerEvents(instance);
        createExternalTaskJobs(instance);
    }


    public void rehydrateProcessInstance(ProcessInstanceEntity entity) {
        ParsedProcessDefinition def = processDefinitions.get(entity.getProcessDefinitionId());
        if (def == null) {
            throw new IllegalStateException("No deployed process definition found for ID: " + entity.getProcessDefinitionId());
        }

        ProcessInstance instance = new ProcessInstance(
                entity.getId(),
                def,
                List.of(entity.getCurrentActivityId()) // Wrap in a list
        );
        instance.putAllVariables(readMap(entity.getVariablesJson()));
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

    private void createAndPersistTask(UserTaskPayload task, String processInstanceId) {
        taskManager.createTask(
                task.taskDefinitionKey(),
                task.name(),
                processInstanceId,
                task.assignee(),
                task.candidateUsers(),
                task.candidateGroups()
        );
        taskManager.getTaskByDefinitionKey(task.taskDefinitionKey(), processInstanceId)
                .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));
    }

    private void scheduleWaitingTimerEvents(ProcessInstance instance) {
        ParsedProcessDefinition definition = instance.getDefinition();
        for (String tokenId : instance.getActiveTokens()) {
            if (definition.isCatchEvent(tokenId)) {
                EventMeta eventMeta = definition.getEvents().get(tokenId);
                if (eventMeta != null && eventMeta.type() == EventMeta.EventType.TIMER) {
                    try {
                        Duration duration = Duration.parse(eventMeta.definitionRef());
                        Instant executionTime = Instant.now().plus(duration);
                        jobScheduler.scheduleJob(instance.getId(), tokenId, executionTime);
                    } catch (Exception e) {
                        log.error("Failed to parse timer duration '{}' for event {}", eventMeta.definitionRef(), tokenId, e);
                    }
                }
            }
        }
    }

    private void createExternalTaskJobs(ProcessInstance instance) {
        ParsedProcessDefinition definition = instance.getDefinition();
        for (String tokenId : instance.getActiveTokens()) {
            if (definition.isServiceTask(tokenId)) {
                ServiceTaskMeta serviceTaskMeta = definition.getServiceTask(tokenId);
                if (serviceTaskMeta != null && serviceTaskMeta.topicName() != null) {
                    ExternalTaskEntity externalTask = new ExternalTaskEntity(instance.getId(), serviceTaskMeta.topicName());
                    externalTaskRepository.save(externalTask);
                    log.info("Created external task {} for topic {}", externalTask.getId(), serviceTaskMeta.topicName());
                }
            }
        }
    }


    private ProcessInstanceEntity convertToEntity(ProcessInstance instance) {
        ProcessInstanceEntity entity = new ProcessInstanceEntity();
        entity.setId(instance.getId());
        entity.setProcessDefinitionId(instance.getDefinition().getId());

        if (instance.getActiveTokens() != null && !instance.getActiveTokens().isEmpty()) {
            entity.setCurrentActivityId(instance.getActiveTokens().get(0));
        }

        if (instance.isCompleted()) {
            entity.setStatus(ProcessInstanceEntity.Status.COMPLETED);
        } else {
            entity.setStatus(ProcessInstanceEntity.Status.RUNNING);
        }

        entity.setVariablesJson(writeMap(instance.getVariables()));
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
        processDefinitions.clear(); // Ensure definitions are cleared between tests
    }

    public ProcessInstance getProcessInstanceById(String id) {
        return instances.get(id);
    }

    public Collection<ProcessInstance> getAllProcessInstances() {
        return instances.values();
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

    private Map<String,Object> readMap(String json) {
        if (json == null || json.isBlank()) return new HashMap<>();
        try { return om.readValue(json, new TypeReference<>() {}); }
        catch (IOException ex) { throw new IllegalStateException("Bad variables_json", ex); }
    }
    private String writeMap(Map<String,Object> m) {
        try { return om.writeValueAsString(m == null ? Map.of() : m); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Serialize variables failed", ex); }
    }

}
