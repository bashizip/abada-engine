package com.abada.engine.core;

import com.abada.engine.core.exception.ProcessEngineException;
import com.abada.engine.dto.ExtendLockRequest;
import com.abada.engine.dto.ExternalTaskFailureDto;
import com.abada.engine.dto.FetchAndLockRequest;
import com.abada.engine.dto.LockedExternalTask;
import com.abada.engine.persistence.entity.ExternalTaskEntity;
import com.abada.engine.persistence.repository.ExternalTaskRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ExternalTaskCommandService {
    private final ExternalTaskRepository repository;
    private final AbadaEngine engine;
    private final ActivityHistoryService history;

    public ExternalTaskCommandService(ExternalTaskRepository repository, AbadaEngine engine,
            ActivityHistoryService history) {
        this.repository = repository;
        this.engine = engine;
        this.history = history;
    }

    @AtomicRuntimeCommand
    public List<LockedExternalTask> fetchAndLock(FetchAndLockRequest request) {
        Instant now = Instant.now();
        for (String topic : request.topics()) {
            var available = repository.findFirstAvailableForUpdate(topic, now);
            if (available.isEmpty()) continue;

            ExternalTaskEntity task = available.get();
            task.setWorkerId(request.workerId());
            task.setStatus(ExternalTaskEntity.Status.LOCKED);
            task.setLockExpirationTime(now.plusMillis(request.lockDuration()));
            repository.save(task);

            ProcessInstance instance = requireInstance(task);
            history.record("EXTERNAL_TASK_LOCKED", instance, task.getActivityId(),
                    Map.of("externalTaskId", task.getId(), "workerId", request.workerId(), "topic", topic));
            return List.of(new LockedExternalTask(task.getId(), task.getTopicName(), instance.getVariables()));
        }
        return List.of();
    }

    @AtomicRuntimeCommand
    public void complete(String id, Map<String, Object> variables) {
        ExternalTaskEntity task = loadForUpdate(id);
        if (task.getStatus() == ExternalTaskEntity.Status.COMPLETED) return;
        if (task.getStatus() != ExternalTaskEntity.Status.LOCKED || task.getLockExpirationTime() == null
                || task.getLockExpirationTime().isBefore(Instant.now())) {
            throw new ProcessEngineException("External task is not locked or its lock has expired: " + id);
        }

        task.setStatus(ExternalTaskEntity.Status.COMPLETED);
        task.setLockExpirationTime(null);
        repository.save(task);
        engine.resumeFromEvent(task.getProcessInstanceId(), task.getActivityId(), variables);
        history.record("EXTERNAL_TASK_COMPLETED", requireInstance(task), task.getActivityId(),
                Map.of("externalTaskId", id, "workerId", valueOrEmpty(task.getWorkerId())));
    }

    @AtomicRuntimeCommand
    public void handleFailure(String id, ExternalTaskFailureDto failure) {
        ExternalTaskEntity task = loadForUpdate(id);
        if (task.getWorkerId() != null && !task.getWorkerId().equals(failure.workerId())) {
            throw new ProcessEngineException("Worker ID mismatch for external task " + id);
        }

        task.setExceptionMessage(failure.errorMessage());
        task.setExceptionStacktrace(failure.errorDetails());
        task.setRetries(failure.retries());
        if (failure.retries() != null && failure.retries() == 0) {
            task.setStatus(ExternalTaskEntity.Status.FAILED);
            task.setLockExpirationTime(null);
        } else {
            boolean delayed = failure.retryTimeout() != null && failure.retryTimeout() > 0;
            task.setStatus(delayed ? ExternalTaskEntity.Status.LOCKED : ExternalTaskEntity.Status.OPEN);
            task.setLockExpirationTime(delayed ? Instant.now().plusMillis(failure.retryTimeout()) : null);
        }
        task.setWorkerId(null);
        repository.save(task);
        history.record("EXTERNAL_TASK_FAILED", requireInstance(task), task.getActivityId(),
                Map.of("externalTaskId", id, "retries", failure.retries() == null ? -1 : failure.retries()));
    }

    @AtomicRuntimeCommand
    public void extendLock(String id, ExtendLockRequest request) {
        ExternalTaskEntity task = loadForUpdate(id);
        if (task.getStatus() != ExternalTaskEntity.Status.LOCKED
                || !request.workerId().equals(task.getWorkerId())) {
            throw new ProcessEngineException("Worker does not own external task lock: " + id);
        }
        task.setLockExpirationTime(Instant.now().plusMillis(request.lockDuration()));
        repository.save(task);
        history.record("EXTERNAL_TASK_LOCK_EXTENDED", requireInstance(task), task.getActivityId(),
                Map.of("externalTaskId", id, "workerId", request.workerId()));
    }

    @AtomicRuntimeCommand
    public void setRetries(String id, int retries) {
        ExternalTaskEntity task = loadForUpdate(id);
        task.setRetries(retries);
        task.setStatus(ExternalTaskEntity.Status.OPEN);
        task.setWorkerId(null);
        task.setLockExpirationTime(null);
        repository.save(task);
        history.record("EXTERNAL_TASK_RETRIES_SET", requireInstance(task), task.getActivityId(),
                Map.of("externalTaskId", id, "retries", retries));
    }

    private ExternalTaskEntity loadForUpdate(String id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ProcessEngineException("External task not found: " + id));
    }

    private ProcessInstance requireInstance(ExternalTaskEntity task) {
        ProcessInstance instance = engine.getProcessInstanceById(task.getProcessInstanceId());
        if (instance == null) {
            throw new IllegalStateException("External task references missing process instance: " + task.getProcessInstanceId());
        }
        return instance;
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
