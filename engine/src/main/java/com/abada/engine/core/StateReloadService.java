package com.abada.engine.core;

import com.abada.engine.persistence.PersistenceService;
import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.core.model.TaskStatus;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.repository.TaskRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class StateReloadService {

    private final PersistenceService persistenceService;
    private final AbadaEngine abadaEngine;
    private final com.abada.engine.observability.EngineMetrics engineMetrics;
    private final TaskRepository taskRepository;

    public StateReloadService(PersistenceService persistenceService, AbadaEngine abadaEngine,
            com.abada.engine.observability.EngineMetrics engineMetrics,
            TaskRepository taskRepository) {
        this.persistenceService = persistenceService;
        this.abadaEngine = abadaEngine;
        this.engineMetrics = engineMetrics;
        this.taskRepository = taskRepository;
    }

    @PostConstruct
    public void reloadStateAtStartup() throws IOException {
        engineMetrics.resetActiveState();
        reloadProcessDefinitions();
        reloadProcessInstances();
        restoreTaskMetrics();
    }

    private void reloadProcessDefinitions() throws IOException {
        List<ProcessDefinitionEntity> definitions = persistenceService.findAllProcessDefinitions();
        for (ProcessDefinitionEntity entity : definitions) {
            abadaEngine.registerPersistedDefinition(entity);
        }
    }

    private void reloadProcessInstances() {
        List<ProcessInstanceEntity> instances = persistenceService.findAllProcessInstances();
        for (ProcessInstanceEntity entity : instances) {
            abadaEngine.rehydrateProcessInstance(entity);
        }
    }

    private void restoreTaskMetrics() {
        taskRepository.countActiveTasksByDefinitionKey(
                        List.of(TaskStatus.AVAILABLE, TaskStatus.CLAIMED))
                .forEach(count -> engineMetrics.restoreActiveTasks(
                        count.getTaskDefinitionKey(), count.getTaskCount()));
    }
}
