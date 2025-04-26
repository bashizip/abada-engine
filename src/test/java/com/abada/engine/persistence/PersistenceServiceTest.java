package com.abada.engine.persistence;

import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class PersistenceServiceTest {

    @Autowired
    private PersistenceService persistenceService;

    @Test
    void testPersistenceAndReloading() {
        // Save ProcessDefinition
        ProcessDefinitionEntity definition = new ProcessDefinitionEntity();
        definition.setId("def-1");
        definition.setName("Test Process");
        definition.setBpmnXml("<bpmn>...</bpmn>");
        persistenceService.saveProcessDefinition(definition);

        // Save ProcessInstance
        ProcessInstanceEntity instance = new ProcessInstanceEntity();
        instance.setId("inst-1");
        instance.setDefinitionId("def-1");
        instance.setStatus("ACTIVE");
        persistenceService.saveProcessInstance(instance);

        // Save Task
        TaskEntity task = new TaskEntity();
        task.setId("task-1");
        task.setProcessInstanceId("inst-1");
        task.setAssignee("user1");
        task.setStatus("PENDING");
        persistenceService.saveTask(task);

        // Verify ProcessDefinition
        ProcessDefinitionEntity loadedDefinition = persistenceService.findProcessDefinitionById("def-1");
        assertThat(loadedDefinition).isNotNull();
        assertThat(loadedDefinition.getName()).isEqualTo("Test Process");

        // Verify ProcessInstance
        ProcessInstanceEntity loadedInstance = persistenceService.findProcessInstanceById("inst-1");
        assertThat(loadedInstance).isNotNull();
        assertThat(loadedInstance.getStatus()).isEqualTo("ACTIVE");

        // Verify Task
        List<TaskEntity> loadedTasks = persistenceService.findTasksByProcessInstanceId("inst-1");
        assertThat(loadedTasks).hasSize(1);
        assertThat(loadedTasks.get(0).getAssignee()).isEqualTo("user1");
    }

    @Test
    void testLoadNonExistentEntities() {
        assertThat(persistenceService.findProcessDefinitionById("nonexistent")).isNull();
        assertThat(persistenceService.findProcessInstanceById("nonexistent")).isNull();
        assertThat(persistenceService.findTasksByProcessInstanceId("nonexistent")).isEmpty();
    }

}
