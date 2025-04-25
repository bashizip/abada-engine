package com.abada.engine.persistence;

import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;

import java.util.List;

public interface PersistenceService {

    void saveProcessDefinition(ProcessDefinitionEntity definition);

    void saveProcessInstance(ProcessInstanceEntity instance);

    void saveTask(TaskEntity task);

    ProcessDefinitionEntity findProcessDefinitionById(String definitionId);

    ProcessInstanceEntity findProcessInstanceById(String instanceId);

    List<TaskEntity> findTasksByProcessInstanceId(String instanceId);
}