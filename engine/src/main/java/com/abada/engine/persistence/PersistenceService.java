package com.abada.engine.persistence;

import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;


import java.util.List;

public interface PersistenceService {

    ProcessInstanceEntity saveOrUpdateProcessInstance(ProcessInstanceEntity instance);

    ProcessDefinitionEntity saveProcessDefinition(ProcessDefinitionEntity definition);

    void saveProcessInstance(ProcessInstanceEntity instance);

    TaskEntity saveTask(TaskEntity task);

    TaskEntity findTaskById(String taskId);

    ProcessDefinitionEntity findProcessDefinitionById(String definitionId);

    ProcessDefinitionEntity findProcessDefinitionByDeploymentId(String deploymentId);

    ProcessInstanceEntity findProcessInstanceById(String instanceId);

    List<TaskEntity> findTasksByProcessInstanceId(String instanceId);

    List<ProcessDefinitionEntity> findAllProcessDefinitions();

    List<ProcessInstanceEntity> findAllProcessInstances();

    List<TaskEntity> findAllTasks();

}
