package com.abada.engine.persistence;

import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

public interface PersistenceService {

    ProcessInstanceEntity saveOrUpdateProcessInstance(ProcessInstanceEntity instance);

    ProcessDefinitionEntity saveProcessDefinition(ProcessDefinitionEntity definition);

    TaskEntity saveTask(TaskEntity task);

    TaskEntity findTaskByIdForUpdate(String taskId);

    ProcessDefinitionEntity findProcessDefinitionById(String definitionId);

    ProcessDefinitionEntity findProcessDefinitionByDeploymentId(String deploymentId);

    ProcessInstanceEntity findProcessInstanceById(String instanceId);

    ProcessInstanceEntity findProcessInstanceByIdForUpdate(String instanceId);

    List<ProcessDefinitionEntity> findAllProcessDefinitions();

    Page<ProcessInstanceEntity> findProcessInstances(Pageable pageable);

    List<ProcessInstanceEntity> findProcessInstancesByIds(Collection<String> instanceIds);

}
