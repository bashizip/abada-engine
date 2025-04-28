package com.abada.engine.core;

import com.abada.engine.persistence.PersistenceService;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StateReloadService {

    private final PersistenceService persistenceService;
    private final AbadaEngine abadaEngine;

    public StateReloadService(PersistenceService persistenceService, AbadaEngine abadaEngine) {
        this.persistenceService = persistenceService;
        this.abadaEngine = abadaEngine;
    }

    @PostConstruct
    public void reloadStateAtStartup() {
        reloadProcessInstances();
        reloadTasks();
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
