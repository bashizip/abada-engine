package com.abada.engine.core;

import com.abada.engine.core.exception.ProcessEngineException;
import com.abada.engine.core.model.EventMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.ServiceTaskMeta;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.core.model.ProcessStatus;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private final ActivityHistoryService historyService;
    private final Map<String, ParsedProcessDefinition> processDefinitions = new ConcurrentHashMap<>();
    private final Map<String, ParsedProcessDefinition> definitionsByDeploymentId = new ConcurrentHashMap<>();
    private final Map<String, String> latestDeploymentByProcessKey = new ConcurrentHashMap<>();

    @Autowired
    public AbadaEngine(PersistenceService persistenceService, TaskManager taskManager, @Lazy EventManager eventManager,
            @Lazy JobScheduler jobScheduler, ExternalTaskRepository externalTaskRepository, ObjectMapper om,
            EngineMetrics engineMetrics, Tracer tracer, ActivityHistoryService historyService) {
        this.persistenceService = persistenceService;
        this.parser = new BpmnParser();
        this.taskManager = taskManager;
        this.eventManager = eventManager;
        this.jobScheduler = jobScheduler;
        this.externalTaskRepository = externalTaskRepository;
        this.om = om;
        this.engineMetrics = engineMetrics;
        this.tracer = tracer;
        this.historyService = historyService;
    }

    @PostConstruct
    public void setup() {
        eventManager.setAbadaEngine(this);
        jobScheduler.setAbadaEngine(this);
    }

    @Transactional
    public ProcessDefinitionEntity deploy(InputStream bpmnXml) {
        Span span = tracer.spanBuilder("abada.process.deploy").startSpan();
        try (var scope = span.makeCurrent()) {
            ParsedProcessDefinition definition = parser.parse(bpmnXml);
            ProcessDefinitionEntity persisted = saveProcessDefinition(definition);
            registerDefinition(definition, persisted);

            span.setAttribute("process.definition.id", definition.getId());
            span.setAttribute("process.definition.name", definition.getName());
            span.setAttribute("process.definition.version", "1.0");

            log.info("Deployed process definition: {}", definition.getId());
            return persisted;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    public List<ProcessDefinitionEntity> getDeployedProcesses() {
        Map<String, ProcessDefinitionEntity> latest = new LinkedHashMap<>();
        persistenceService.findAllProcessDefinitions()
                .forEach(definition -> latest.putIfAbsent(definition.getProcessKey(), definition));
        return List.copyOf(latest.values());
    }

    public Optional<ProcessDefinitionEntity> getProcessDefinitionById(String id) {
        return Optional.ofNullable(persistenceService.findProcessDefinitionById(id));
    }

    public ParsedProcessDefinition getParsedProcessDefinition(String processDefinitionId) {
        return processDefinitions.get(processDefinitionId);
    }

    /** Registers a definition read from storage without creating a new deployment. */
    public void registerPersistedDefinition(ProcessDefinitionEntity entity) {
        ParsedProcessDefinition definition = parser.parse(new java.io.ByteArrayInputStream(
                entity.getBpmnXml().getBytes(StandardCharsets.UTF_8)));
        definitionsByDeploymentId.put(entity.getDeploymentId(), definition);
        latestDeploymentByProcessKey.compute(entity.getProcessKey(), (key, currentDeploymentId) -> {
            if (currentDeploymentId == null) {
                processDefinitions.put(key, definition);
                return entity.getDeploymentId();
            }
            ProcessDefinitionEntity current = persistenceService.findProcessDefinitionByDeploymentId(currentDeploymentId);
            if (current == null || entity.getVersion() > current.getVersion()) {
                processDefinitions.put(key, definition);
                return entity.getDeploymentId();
            }
            return currentDeploymentId;
        });
    }

    private void registerDefinition(ParsedProcessDefinition definition, ProcessDefinitionEntity entity) {
        processDefinitions.put(entity.getProcessKey(), definition);
        definitionsByDeploymentId.put(entity.getDeploymentId(), definition);
        latestDeploymentByProcessKey.put(entity.getProcessKey(), entity.getDeploymentId());
    }

    @Transactional
    public ProcessInstance startProcess(@SpanTag("process.definition.id") String processDefinitionId, String username) {
        return startProcess(processDefinitionId, username, Map.of());
    }

    @Transactional
    public ProcessInstance startProcess(@SpanTag("process.definition.id") String processDefinitionId, String username,
            Map<String, Object> initialVariables) {
        Timer.Sample sample = engineMetrics.startProcessTimer();
        Span span = tracer.spanBuilder("abada.process.start").startSpan();

        try (var scope = span.makeCurrent()) {
            ParsedProcessDefinition definition = processDefinitions.get(processDefinitionId);
            if (definition == null) {
                throw new ProcessEngineException("Unknown process ID: " + processDefinitionId);
            }

            ProcessInstance instance = new ProcessInstance(definition);
            instance.setProcessDefinitionDeploymentId(latestDeploymentByProcessKey.get(processDefinitionId));
            instance.putAllVariables(initialVariables);
            instance.setStartedBy(username != null && !username.isBlank() ? username : "system");

            span.setAttribute("process.instance.id", instance.getId());
            span.setAttribute("process.definition.id", processDefinitionId);
            span.setAttribute("process.definition.name", definition.getName());

            engineMetrics.recordProcessStarted(processDefinitionId);
            log.info("Started process instance: {} of definition: {} by user: {}",
                    instance.getId(), processDefinitionId, username != null ? username : "system");

            ProcessInstanceEntity entity = convertToEntity(instance);
            entity.setStartedBy(instance.getStartedBy());
            persistRuntimeState(instance, entity);

            List<UserTaskPayload> userTasks = instance.advance();
            if (instance.isCompleted() && instance.getEndDate() == null) {
                instance.setEndDate(Instant.now());
                engineMetrics.recordProcessCompleted(processDefinitionId);
            }

            entity = convertToEntity(instance);
            entity.setStartedBy(instance.getStartedBy());
            persistRuntimeState(instance, entity);

            for (UserTaskPayload task : userTasks) {
                createAndPersistTask(task, instance.getId());
            }

            eventManager.registerWaitStates(instance);
            scheduleWaitingTimerEvents(instance);
            createExternalTaskJobs(instance);
            historyService.record("PROCESS_STARTED", instance, definition.getStartEventId(), Map.of());

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

    @Transactional
    public void claim(String taskId, String user, List<String> groups) {
        TaskInstance task = loadTaskForUpdate(taskId);
        taskManager.claimTask(task, user, groups);
        persistTask(task);
        historyService.record("TASK_CLAIMED", loadProcessInstance(task.getProcessInstanceId()),
                task.getTaskDefinitionKey(), Map.of("assignee", user));
    }

    @Transactional
    public void completeTask(String taskId, String user, List<String> groups, Map<String, Object> variables) {
        log.info("Completing task {} with variables: {}", taskId, variables);
        TaskInstance currentTask = loadTaskForUpdate(taskId);
        taskManager.checkCanComplete(currentTask, user, groups);

        String processInstanceId = currentTask.getProcessInstanceId();
        ProcessInstanceEntity authoritativeInstance =
                persistenceService.findProcessInstanceByIdForUpdate(processInstanceId);
        if (authoritativeInstance == null) {
            // This is an internal consistency error, not a client error.
            throw new IllegalStateException("No process instance found for id=" + processInstanceId);
        }
        ProcessInstance instance = materializeProcessInstance(authoritativeInstance);

        if (instance.isSuspended()) {
            throw new ProcessEngineException("Process instance is suspended: " + processInstanceId);
        }

        if (variables != null && !variables.isEmpty()) {
            instance.putAllVariables(variables);
        }

        taskManager.completeTask(currentTask);
        persistTask(currentTask);
        historyService.record("TASK_COMPLETED", instance, currentTask.getTaskDefinitionKey(), Map.of());
        persistRuntimeState(instance);

        List<UserTaskPayload> nextTasks = instance.advance(currentTask.getTaskDefinitionKey());
        if (instance.isCompleted() && instance.getEndDate() == null) {
            instance.setEndDate(Instant.now());
        }
        persistRuntimeState(instance);

        for (UserTaskPayload task : nextTasks) {
            TaskInstance createdTask = taskManager.createTaskSnapshot(
                    task.taskDefinitionKey(), task.name(), processInstanceId, task.assignee(),
                    task.candidateUsers(), task.candidateGroups());
            persistTask(createdTask);
        }

        eventManager.registerWaitStates(instance);
        scheduleWaitingTimerEvents(instance);
        createExternalTaskJobs(instance);
    }

    @Transactional
    public void failTask(String taskId) {
        TaskInstance task = loadTaskForUpdate(taskId);
        taskManager.failTask(task);
        persistTask(task);
        historyService.record("TASK_FAILED", loadProcessInstance(task.getProcessInstanceId()),
                task.getTaskDefinitionKey(), Map.of());
    }

    @Transactional
    public boolean failProcess(String processInstanceId) {
        ProcessInstance instance = loadProcessInstanceForUpdate(processInstanceId);
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

        persistRuntimeState(instance);
        historyService.record("PROCESS_FAILED", instance, null, Map.of());
        return true;
    }

    @Transactional
    public void cancelProcessInstance(String processInstanceId, String reason) {
        log.info("Cancelling process instance {} for reason: {}", processInstanceId, reason);
        ProcessInstance instance = loadProcessInstanceForUpdate(processInstanceId);
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

        persistRuntimeState(instance);
        historyService.record("PROCESS_CANCELLED", instance, null, Map.of("reason", reason == null ? "" : reason));
    }

    @Transactional
    public void suspendProcessInstance(String processInstanceId, boolean suspended) {
        log.info("Setting suspension state for process instance {} to {}", processInstanceId, suspended);
        ProcessInstance instance = loadProcessInstanceForUpdate(processInstanceId);
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

        persistRuntimeState(instance);
        historyService.record(suspended ? "PROCESS_SUSPENDED" : "PROCESS_RESUMED", instance, null, Map.of());
    }

    @Transactional
    public void updateProcessVariables(String processInstanceId, Map<String, Object> modifications) {
        ProcessInstance instance = loadProcessInstanceForUpdate(processInstanceId);
        if (instance == null) throw new ProcessEngineException("Process instance not found: " + processInstanceId);
        instance.putAllVariables(modifications);
        persistRuntimeState(instance);
        historyService.record("VARIABLES_UPDATED", instance, null,
                Map.of("variableNames", modifications.keySet()));
    }

    @Transactional
    public void resumeFromEvent(String processInstanceId, String eventId, Map<String, Object> variables) {
        log.info("Resuming process instance {} from event {}", processInstanceId, eventId);
        ProcessInstance instance = loadProcessInstanceForUpdate(processInstanceId);
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
        persistRuntimeState(instance);
        historyService.record("EVENT_CORRELATED", instance, eventId, Map.of());

        for (UserTaskPayload task : nextTasks) {
            createAndPersistTask(task, processInstanceId);
        }

        eventManager.registerWaitStates(instance);
        scheduleWaitingTimerEvents(instance);
        createExternalTaskJobs(instance);
    }

    private ProcessInstance materializeProcessInstance(ProcessInstanceEntity entity) {
        ParsedProcessDefinition def = entity.getProcessDefinitionDeploymentId() == null
                ? processDefinitions.get(entity.getProcessDefinitionId())
                : definitionsByDeploymentId.get(entity.getProcessDefinitionDeploymentId());
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
        instance.setProcessDefinitionDeploymentId(entity.getProcessDefinitionDeploymentId());
        instance.setEntityVersion(entity.getEntityVersion());
        instance.setStartedBy(entity.getStartedBy());
        instance.putAllVariables(readMap(entity.getVariablesJson()));
        instance.setActiveTokens(readList(entity.getActiveTokensJson(), activeTokens));
        instance.setJoinExpectedTokens(readIntegerMap(entity.getJoinExpectedTokensJson()));
        instance.setJoinArrivedTokens(readSetMap(entity.getJoinArrivedTokensJson()));
        return instance;
    }

    private void createAndPersistTask(UserTaskPayload task, String processInstanceId) {
        TaskInstance createdTask = taskManager.createTaskSnapshot(
                task.taskDefinitionKey(),
                task.name(),
                processInstanceId,
                task.assignee(),
                task.candidateUsers(),
                task.candidateGroups());
        persistTask(createdTask);
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
                    if (externalTaskRepository.existsByProcessInstanceIdAndActivityIdAndStatusIn(instance.getId(),
                            tokenId, List.of(ExternalTaskEntity.Status.OPEN, ExternalTaskEntity.Status.LOCKED))) {
                        continue;
                    }
                    ExternalTaskEntity externalTask = new ExternalTaskEntity(instance.getId(),
                            serviceTaskMeta.topicName());
                    externalTask.setActivityId(tokenId);
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
        entity.setProcessDefinitionDeploymentId(instance.getProcessDefinitionDeploymentId());

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
        entity.setStartedBy(instance.getStartedBy());
        entity.setActiveTokensJson(writeValue(instance.getActiveTokens()));
        entity.setJoinExpectedTokensJson(writeValue(instance.getJoinExpectedTokens()));
        entity.setJoinArrivedTokensJson(writeValue(instance.getJoinArrivedTokens()));
        entity.setEntityVersion(instance.getEntityVersion());
        return entity;
    }

    private void persistRuntimeState(ProcessInstance instance) {
        persistRuntimeState(instance, convertToEntity(instance));
    }

    private void persistRuntimeState(ProcessInstance instance, ProcessInstanceEntity entity) {
        ProcessInstanceEntity saved = persistenceService.saveOrUpdateProcessInstance(entity);
        instance.setEntityVersion(saved.getEntityVersion());
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
        entity.setEntityVersion(taskInstance.getEntityVersion());

        return entity;
    }

    private TaskInstance loadTaskForUpdate(String taskId) {
        TaskEntity entity = persistenceService.findTaskByIdForUpdate(taskId);
        if (entity == null) {
            throw new ProcessEngineException("Task not found: " + taskId);
        }
        return taskManager.materialize(entity);
    }

    private ProcessInstance loadProcessInstance(String processInstanceId) {
        ProcessInstanceEntity entity = persistenceService.findProcessInstanceById(processInstanceId);
        if (entity == null) {
            throw new IllegalStateException("No process instance found for id=" + processInstanceId);
        }
        return materializeProcessInstance(entity);
    }

    private ProcessInstance loadProcessInstanceForUpdate(String processInstanceId) {
        ProcessInstanceEntity entity = persistenceService.findProcessInstanceByIdForUpdate(processInstanceId);
        return entity == null ? null : materializeProcessInstance(entity);
    }

    private void persistTask(TaskInstance task) {
        TaskEntity saved = persistenceService.saveTask(convertToEntity(task));
        task.setEntityVersion(saved.getEntityVersion());
    }

    public void clearMemory() {
        processDefinitions.clear(); // Ensure definitions are cleared between tests
        definitionsByDeploymentId.clear();
        latestDeploymentByProcessKey.clear();
    }

    @Transactional(readOnly = true)
    public ProcessInstance getProcessInstanceById(@SpanTag("process.instance.id") String id) {
        Span span = tracer.spanBuilder("abada.process.get").startSpan();
        try (var scope = span.makeCurrent()) {
            span.setAttribute("process.instance.id", id);
            ProcessInstanceEntity entity = persistenceService.findProcessInstanceById(id);
            ProcessInstance instance = entity == null ? null : materializeProcessInstance(entity);
            if (instance != null) {
                span.setAttribute("process.definition.id", instance.getDefinition().getId());
                span.setAttribute("process.status", instance.getStatus().toString());
            }
            return instance;
        } finally {
            span.end();
        }
    }

    @Transactional(readOnly = true)
    public Collection<ProcessInstance> getAllProcessInstances() {
        return persistenceService.findAllProcessInstances().stream()
                .map(this::materializeProcessInstance)
                .toList();
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public Optional<TaskInstance> getTaskById(String taskId) {
        return taskManager.getTask(taskId);
    }

    public ProcessDefinitionEntity saveProcessDefinition(ParsedProcessDefinition definition) {
        String checksum = sha256(definition.getRawXml());
        ProcessDefinitionEntity latest = persistenceService.findProcessDefinitionById(definition.getId());
        if (latest != null && checksum.equals(latest.getChecksum())) {
            return latest;
        }
        ProcessDefinitionEntity entity = new ProcessDefinitionEntity();
        entity.setId(definition.getId());
        entity.setVersion(latest == null ? 1 : latest.getVersion() + 1);
        entity.setChecksum(checksum);
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

        return persistenceService.saveProcessDefinition(entity);
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

    private String writeValue(Object value) {
        try {
            return om.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Serialize runtime state failed", ex);
        }
    }

    private List<String> readList(String json, List<String> fallback) {
        if (json == null || json.isBlank()) return fallback;
        try {
            return om.readValue(json, new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Bad active_tokens_json", ex);
        }
    }

    private Map<String, Integer> readIntegerMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return om.readValue(json, new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Bad join_expected_tokens_json", ex);
        }
    }

    private Map<String, Set<String>> readSetMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return om.readValue(json, new TypeReference<>() {});
        } catch (IOException ex) {
            throw new IllegalStateException("Bad join_arrived_tokens_json", ex);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

}
