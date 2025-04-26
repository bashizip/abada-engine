package com.abada.engine.persistence;

import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

//@Profile("dev")
@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final PersistenceService persistenceService;

    public DatabaseInitializer(PersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("[AbadaEngine] Preloading demo data into H2 database...");

        // Create and save a Process Definition
        ProcessDefinitionEntity definition = new ProcessDefinitionEntity();
        definition.setId("demo-proc-1");
        definition.setName("Demo Process 1");
        definition.setBpmnXml("<bpmn>Demo BPMN XML</bpmn>");
        persistenceService.saveProcessDefinition(definition);

        // Create and save a Process Instance
        ProcessInstanceEntity instance = new ProcessInstanceEntity();
        instance.setId("demo-inst-1");
        instance.setDefinitionId("demo-proc-1");
        instance.setStatus("ACTIVE");
        persistenceService.saveProcessInstance(instance);

        // Create and save a Task
        TaskEntity task = new TaskEntity();
        task.setId("demo-task-1");
        task.setProcessInstanceId("demo-inst-1");
        task.setAssignee("admin");
        task.setStatus("PENDING");
        persistenceService.saveTask(task);

        System.out.println("[AbadaEngine] Demo data preloaded successfully.");
    }
}
