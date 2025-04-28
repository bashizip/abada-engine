package com.abada.engine.persistence;

import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class PersistenceServiceTest {

    @Autowired
    private PersistenceService persistenceService;

    @Test
    void testPersistenceAndReloading() {
        // Create and save a ProcessInstanceEntity
        ProcessInstanceEntity processInstance = new ProcessInstanceEntity();
        processInstance.setId(UUID.randomUUID().toString());
        processInstance.setProcessDefinitionId("test-process");
        processInstance.setCurrentActivityId("userTask1");
        processInstance.setStatus(ProcessInstanceEntity.Status.RUNNING);

        persistenceService.saveOrUpdateProcessInstance(processInstance);

        // Reload from DB
        ProcessInstanceEntity loadedInstance = persistenceService.findProcessInstanceById(processInstance.getId());
        assertThat(loadedInstance).isNotNull();
        assertThat(loadedInstance.getProcessDefinitionId()).isEqualTo("test-process");
        assertThat(loadedInstance.getCurrentActivityId()).isEqualTo("userTask1");
        assertThat(loadedInstance.getStatus()).isEqualTo(ProcessInstanceEntity.Status.RUNNING);

        // Create and save a TaskEntity
        TaskEntity task = new TaskEntity();
        task.setId(UUID.randomUUID().toString());
        task.setProcessInstanceId(processInstance.getId());
        task.setTaskDefinitionKey("approveTask");
        task.setName("Approve Task");
        task.setAssignee(null); // not claimed yet
        task.setStatus(TaskEntity.Status.CREATED);

        persistenceService.saveTask(task);

        // Reload from DB
        List<TaskEntity> loadedTasks = persistenceService.findTasksByProcessInstanceId(processInstance.getId());
        assertThat(loadedTasks).isNotEmpty();
        TaskEntity loadedTask = loadedTasks.get(0);
        assertThat(loadedTask.getTaskDefinitionKey()).isEqualTo("approveTask");
        assertThat(loadedTask.getName()).isEqualTo("Approve Task");
        assertThat(loadedTask.getStatus()).isEqualTo(TaskEntity.Status.CREATED);
    }

    @Test
    void testLoadNonExistentEntities() {
        assertThat(persistenceService.findProcessInstanceById("non-existent-id")).isNull();
        assertThat(persistenceService.findProcessDefinitionById("non-existent-id")).isNull();
    }
}
