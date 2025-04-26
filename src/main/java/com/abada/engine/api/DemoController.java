package com.abada.engine.api;

import com.abada.engine.persistence.PersistenceService;
import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/demo")
public class DemoController {

    private final PersistenceService persistenceService;

    public DemoController(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @GetMapping("/process-definitions")
    public List<ProcessDefinitionEntity> getAllProcessDefinitions() {
        return persistenceService.findAllProcessDefinitions();
    }

    @GetMapping("/process-instances")
    public List<ProcessInstanceEntity> getAllProcessInstances() {
        return persistenceService.findAllProcessInstances();
    }

    @GetMapping("/tasks")
    public List<TaskEntity> getAllTasks() {
        return persistenceService.findAllTasks();
    }
}
