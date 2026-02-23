package com.abada.engine.core;

import com.abada.engine.core.exception.ProcessEngineException;
import com.abada.engine.core.model.EventMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.ServiceTaskMeta;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.core.model.ProcessStatus;
import com.abada.engine.core.model.TaskStatus;
import com.abada.engine.dto.UserTaskPayload;
import com.abada.engine.observability.EngineMetrics;
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
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.annotation.SpanTag;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
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
    private final EngineMetrics engineMetrics;
    private final Tracer tracer;
    private final Map<String, ParsedProcessDefinition> processDefinitions = new HashMap<>();
    private final Map<String, ProcessInstance> instances = new HashMap<>();

    @Autowired
    public AbadaEngine(PersistenceService persistenceService, TaskManager taskManager, @Lazy EventManager eventManager,
            @Lazy JobScheduler jobScheduler, ExternalTaskRepository externalTaskRepository, ObjectMapper om,
            EngineMetrics engineMetrics, Tracer tracer) {
        this.persistenceService = persistenceService;
        this.parser = new BpmnParser();
        this.taskManager = taskManager;
        this.eventManager = eventManager;
        this.jobScheduler = jobScheduler;
        this.externalTaskRepository = externalTaskRepository;
        this.om = om;
        this.engineMetrics = engineMetrics;
        this.tracer = tracer;
    }

    @PostConstruct
    public void setup() {
        eventManager.setAbadaEngine(this);
        jobScheduler.setAbadaEngine(this);
    }

    public void deploy(InputStream bpmnXml) {
        Span span = tracer.spanBuilder("abada.process.deploy").startSpan();
        try (var scope = span.makeCurrent()) {
            ParsedProcessDefinition definition = parser.parse(bpmnXml);
            processDefinitions.put(definition.getId(), definition);
            saveProcessDefinition(definition);

            span.setAttribute("process.definition.id", definition.getId());
            span.setAttribute("process.definition.name", definition.getName());
            span.setAttribute("process.definition.version", "1.0");

            log.info("Deployed process definition: {}", definition.getId());
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
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

    public ProcessInstance startProcess(@SpanTag("process.definition.id") String processDefinitionId, String username) {
        Timer.Sample sample = engineMetrics.startProcessTimer();
        Span span = tracer.spanBuilder("abada.process.start").startSpan();

        try (var scope = span.makeCurrent()) {
            ParsedProcessDefinition definition = processDefinitions.get(processDefinitionId);
            if (definition == null) {
                throw new ProcessEngineException("Unknown process ID: " + processDefinitionId);
            }

            ProcessInstance instance = new ProcessInstance(definition);
            instances.put(instance.getId(), instance);

            span.setAttribute("process.instance.id", instance.getId());
            span.setAttribute("process.definition.id", processDefinitionId);
            span.setAttribute("process.definition.name", definition.getName());

            engineMetrics.recordProcessStarted(processDefinitionId);
            log.info("Started process instance: {} of definition: {} by user: {}",
                    instance.getId(), processDefinitionId, username != null ? username : "system");

            ProcessInstanceEntity entity = convertToEntity(instance);
            entity.setStartedBy(username != null && !username.isBlank() ? username : "system");
            persistenceService.saveOrUpdateProcessInstance(entity);

            List<UserTaskPayload> userTasks = instance.advance();
            if (instance.isCompleted() && instance.getEndDate() == null) {
                instance.setEndDate(Instant.now());
                engineMetrics.recordProcessCompleted(processDefinitionId);
            }

            entity = convertToEntity(instance);
            entity.setStartedBy(username != null && !username.isBlank() ? username : "system");
            persistenceService.saveOrUpdateProcessInstance(entity);

            for (UserTaskPayload task : userTasks) {
                createAndPersistTask(task, instance.getId());
            }

            eventManager.registerWaitStates(instance);
            scheduleWaitingTimerEvents(instance);
            createExternalTaskJobs(instance);

            engineMetrics.recordProcessDuration(sample, processDefinitionId);
            return instance;
        } catch (Exception e) {
            engineMetrics.recordProcessFailed(processDefinitionId);
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    // Overloaded version for backward compatibility
    public ProcessInstance startProcess(@SpanTag("process.definition.id") String processDefinitionId) {
        return startProcess(processDefinitionId, null);
    }

    public void claim(String taskId, String user, List<String> groups) {
        taskManager.claimTask(taskId, user, groups);
        taskManager.getTask(taskId)
                .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));
    }

    public void completeTask(String taskId, String user, List<String> groups, Map<String, Object> variables) {
        log.info("Completing task {} with variables: {}", taskId, variables);

        taskManager.checkCanComplete(taskId, user, groups);

        TaskInstance currentTask = taskManager.getTask(taskId)
                .orElseThrow(() -> new ProcessEngineException("Task not found: " + taskId));

        String processInstanceId = currentTask.getProcessInstanceId();
        ProcessInstance instance = instances.get(processInstanceId);
        if (instance == null) {
            // This is an internal consistency error, not a client error.
            throw new IllegalStateException("No process instance found for id=" + processInstanceId);
        }

        if (instance.isSuspended()) {
            throw new ProcessEngineException("Process instance is suspended: " + processInstanceId);
        }

        if (variables != null && !variables.isEmpty()) {
            instance.putAllVariables(variables);
        }

        taskManager.completeTask(taskId);
        persistenceService.saveTask(convertToEntity(currentTask));
        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        List<UserTaskPayload> nextTasks = instance.advance(currentTask.getTaskDefinitionKey());
        if (instance.isCompleted() && instance.getEndDate() == null) {
            instance.setEndDate(Instant.now());
        }
        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));

        for (UserTaskPayload task : nextTasks) {
            createAndPersistTask(task, processInstanceId);
        }

        eventManager.registerWaitStates(instance);
        scheduleWaitingTimerEvents(instance);
        createExternalTaskJobs(instance);
    }

    public void failTask(String taskId) {
        taskManager.failTask(taskId);
        taskManager.getTask(taskId)
                .ifPresent(taskInstance -> persistenceService.saveTask(convertToEntity(taskInstance)));
    }

    public boolean failProcess(String processInstanceId) {
        ProcessInstance instance = instances.get(processInstanceId);
        if (instance == null || instance.getStatus() == ProcessStatus.COMPLETED
                || instance.getStatus() == ProcessStatus.FAILED
                || instance.getStatus() == ProcessStatus.CANCELLED) {
            log.warn("Process instance {} not found or already in a terminal state.", processInstanceId);
            return false;
        }

        log.info("Failing process instance {}", processInstanceId);
        instance.setStatus(ProcessStatus.FAILED);
        instance.setEndDate(Instant.now());
        instance.setActiveTokens(Collections.emptyList()); // Clear active tokens to stop execution

        // Record metrics for process failure
        engineMetrics.recordProcessFailed(instance.getDefinition().getId());

        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));
        return true;
    }

    public void cancelProcessInstance(String processInstanceId, String reason) {
        log.info("Cancelling process instance {} for reason: {}", processInstanceId, reason);
        ProcessInstance instance = instances.get(processInstanceId);
        if (instance == null) {
            throw new ProcessEngineException("Process instance not found: " + processInstanceId);
        }

        if (instance.getStatus() == ProcessStatus.COMPLETED || instance.getStatus() == ProcessStatus.FAILED
                || instance.getStatus() == ProcessStatus.CANCELLED) {
            throw new ProcessEngineException(
                    "Process instance is already in a terminal state: " + instance.getStatus());
        }

        instance.setStatus(ProcessStatus.CANCELLED);
        instance.setEndDate(Instant.now());
        instance.setActiveTokens(Collections.emptyList());

        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));
    }

    public void suspendProcessInstance(String processInstanceId, boolean suspended) {
        log.info("Setting suspension state for process instance {} to {}", processInstanceId, suspended);
        ProcessInstance instance = instances.get(processInstanceId);
        if (instance == null) {
            throw new ProcessEngineException("Process instance not found: " + processInstanceId);
        }

        if (suspended) {
            // Only suspend if not already in a terminal state
            if (instance.getStatus() != ProcessStatus.COMPLETED
                    && instance.getStatus() != ProcessStatus.FAILED
                    && instance.getStatus() != ProcessStatus.CANCELLED) {
                instance.setSuspended(true);
                instance.setStatus(ProcessStatus.SUSPENDED);
            }
        } else {
            // Resume: restore to RUNNING if currently SUSPENDED
            if (instance.getStatus() == ProcessStatus.SUSPENDED) {
                instance.setSuspended(false);
                instance.setStatus(ProcessStatus.RUNNING);
            }
        }

        persistenceService.saveOrUpdateProcessInstance(convertToEntity(instance));
    }

    public void resumeFromEvent(String processInstanceId, String eventId, Map<String, Object> variables) {
        log.info("Resuming process instance {} from event {}", processInstanceId, eventId);
        ProcessInstance instance = instances.get(processInstanceId);
        if (instance == null) {
            throw new ProcessEngineException("No process instance found for id=" + processInstanceId);
        }

        if (instance.isSuspended()) {
            throw new ProcessEngineException("Process instance is suspended: " + processInstanceId);
        }

        if (variables != null && !variables.isEmpty()) {
            instance.putAllVariables(variables);
        }

        List<UserTaskPayload> nextTasks = instance.advance(eventId);
        if (instance.isCompleted() && instance.getEndDate() == null) {
            instance.setEndDate(Instant.now());
        }
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
            throw new IllegalStateException(
                    "No deployed process definition found for ID: " + entity.getProcessDefinitionId());
        }

        List<String> activeTokens = entity.getCurrentActivityId() != null ? List.of(entity.getCurrentActivityId())
                : Collections.emptyList();

        ProcessInstance instance = new ProcessInstance(
                entity.getId(),
                def,
                activeTokens,
                entity.getStartDate(),
                entity.getEndDate());
        instance.setStatus(entity.getStatus());
        instance.setSuspended(entity.isSuspended());
        instance.putAllVariables(readMap(entity.getVariablesJson()));
        instances.put(instance.getId(), instance);

        // Restore metrics for active processes
        if (instance.getStatus() == ProcessStatus.RUNNING || instance.getStatus() == ProcessStatus.SUSPENDED) {
            engineMetrics.restoreActiveProcess(instance.getDefinition().getId());
        }
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
        task.setStatus(entity.getStatus());
        task.setStartDate(entity.getStartDate());
        task.setEndDate(entity.getEndDate());

        taskManager.addTask(task);

        // Restore metrics for active tasks
        if (task.getStatus() == TaskStatus.AVAILABLE || task.getStatus() == TaskStatus.CLAIMED) {
            engineMetrics.restoreActiveTask(task.getTaskDefinitionKey());
        }
    }

    private void createAndPersistTask(UserTaskPayload task, String processInstanceId) {
        taskManager.createTask(
                task.taskDefinitionKey(),
                task.name(),
                processInstanceId,
                task.assignee(),
                task.candidateUsers(),
                task.candidateGroups());
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
                        log.error("Failed to parse timer duration '{}' for event {}", eventMeta.definitionRef(),
                                tokenId, e);
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
                    ExternalTaskEntity externalTask = new ExternalTaskEntity(instance.getId(),
                            serviceTaskMeta.topicName());
                    externalTaskRepository.save(externalTask);
                    log.info("Created external task {} for topic {}", externalTask.getId(),
                            serviceTaskMeta.topicName());
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
        } else {
            entity.setCurrentActivityId(null);
        }

        entity.setStatus(instance.getStatus());
        entity.setSuspended(instance.isSuspended());

        entity.setStartDate(instance.getStartDate());
        entity.setEndDate(instance.getEndDate());
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
        entity.setStatus(taskInstance.getStatus());
        entity.setStartDate(taskInstance.getStartDate());
        entity.setEndDate(taskInstance.getEndDate());

        entity.setCandidateUsers(new ArrayList<>(taskInstance.getCandidateUsers()));
        entity.setCandidateGroups(new ArrayList<>(taskInstance.getCandidateGroups()));

        return entity;
    }

    public void clearMemory() {
        instances.clear();
        taskManager.clearTasks();
        processDefinitions.clear(); // Ensure definitions are cleared between tests
    }

    public ProcessInstance getProcessInstanceById(@SpanTag("process.instance.id") String id) {
        Span span = tracer.spanBuilder("abada.process.get").startSpan();
        try (var scope = span.makeCurrent()) {
            span.setAttribute("process.instance.id", id);
            ProcessInstance instance = instances.get(id);
            if (instance != null) {
                span.setAttribute("process.definition.id", instance.getDefinition().getId());
                span.setAttribute("process.status", instance.getStatus().toString());
            }
            return instance;
        } finally {
            span.end();
        }
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
        entity.setDocumentation(definition.getDocumentation());
        entity.setBpmnXml(definition.getRawXml());

        // Save candidate starter groups and users as comma-separated strings
        if (definition.getCandidateStarterGroups() != null && !definition.getCandidateStarterGroups().isEmpty()) {
            entity.setCandidateStarterGroups(String.join(",", definition.getCandidateStarterGroups()));
        }
        if (definition.getCandidateStarterUsers() != null && !definition.getCandidateStarterUsers().isEmpty()) {
            entity.setCandidateStarterUsers(String.join(",", definition.getCandidateStarterUsers()));
        }

        persistenceService.saveProcessDefinition(entity);
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank())
            return new HashMap<>();
        try {
            return om.readValue(json, new TypeReference<>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("Bad variables_json", ex);
        }
    }

    private String writeMap(Map<String, Object> m) {
        try {
            return om.writeValueAsString(m == null ? Map.of() : m);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Serialize variables failed", ex);
        }
    }

}
