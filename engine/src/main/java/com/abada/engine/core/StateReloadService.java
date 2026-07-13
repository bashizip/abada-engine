package com.abada.engine.core;

import com.abada.engine.persistence.PersistenceService;
import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class StateReloadService {

    private final PersistenceService persistenceService;
    private final AbadaEngine abadaEngine;
    private final com.abada.engine.observability.EngineMetrics engineMetrics;

    public StateReloadService(PersistenceService persistenceService, AbadaEngine abadaEngine,
            com.abada.engine.observability.EngineMetrics engineMetrics) {
        this.persistenceService = persistenceService;
        this.abadaEngine = abadaEngine;
        this.engineMetrics = engineMetrics;
    }

    @PostConstruct
    public void reloadStateAtStartup() throws IOException {
        engineMetrics.resetActiveState();
        reloadProcessDefinitions();  // 🚨 Reload definitions FIRST
        reloadProcessInstances();    // 🚨 Then reload instances
        reloadTasks();               // 🚨 Then reload tasks
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

    private void reloadTasks() {
        List<TaskEntity> tasks = persistenceService.findAllTasks();
        for (TaskEntity taskEntity : tasks) {
            abadaEngine.rehydrateTaskInstance(taskEntity);
        }
    }
}
