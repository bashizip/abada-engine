package com.abada.engine.core;

import com.abada.engine.core.model.TaskStatus;
import com.abada.engine.core.model.ProcessStatus;
import com.abada.engine.persistence.repository.ProcessInstanceRepository;
import com.abada.engine.persistence.repository.TaskRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StateReloadService {

    private final com.abada.engine.observability.EngineMetrics engineMetrics;
    private final TaskRepository taskRepository;
    private final ProcessInstanceRepository processInstanceRepository;

    public StateReloadService(com.abada.engine.observability.EngineMetrics engineMetrics,
            TaskRepository taskRepository,
            ProcessInstanceRepository processInstanceRepository) {
        this.engineMetrics = engineMetrics;
        this.taskRepository = taskRepository;
        this.processInstanceRepository = processInstanceRepository;
    }

    @PostConstruct
    public void reloadStateAtStartup() {
        engineMetrics.resetActiveState();
        restoreProcessMetrics();
        restoreTaskMetrics();
    }

    private void restoreProcessMetrics() {
        processInstanceRepository.countActiveProcessesByDefinitionId(
                        List.of(ProcessStatus.RUNNING, ProcessStatus.SUSPENDED))
                .forEach(count -> engineMetrics.restoreActiveProcesses(
                        count.getProcessDefinitionId(), count.getInstanceCount()));
    }

    private void restoreTaskMetrics() {
        taskRepository.countActiveTasksByDefinitionKey(
                        List.of(TaskStatus.AVAILABLE, TaskStatus.CLAIMED))
                .forEach(count -> engineMetrics.restoreActiveTasks(
                        count.getTaskDefinitionKey(), count.getTaskCount()));
    }
}
