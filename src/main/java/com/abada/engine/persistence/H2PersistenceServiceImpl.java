package com.abada.engine.persistence;

import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import com.abada.engine.persistence.repository.ProcessDefinitionRepository;
import com.abada.engine.persistence.repository.ProcessInstanceRepository;
import com.abada.engine.persistence.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class H2PersistenceServiceImpl implements PersistenceService {

    private final ProcessDefinitionRepository processDefinitionRepository;
    private final ProcessInstanceRepository processInstanceRepository;
    private final TaskRepository taskRepository;

    public H2PersistenceServiceImpl(ProcessDefinitionRepository processDefinitionRepository,
                                    ProcessInstanceRepository processInstanceRepository,
                                    TaskRepository taskRepository) {
        this.processDefinitionRepository = processDefinitionRepository;
        this.processInstanceRepository = processInstanceRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public void saveProcessDefinition(ProcessDefinitionEntity definition) {
        processDefinitionRepository.save(definition);
    }

    @Override
    public void saveProcessInstance(ProcessInstanceEntity instance) {
        processInstanceRepository.save(instance);
    }

    @Override
    public void saveTask(TaskEntity task) {
        taskRepository.save(task);
    }

    @Override
    public ProcessDefinitionEntity findProcessDefinitionById(String definitionId) {
        return processDefinitionRepository.findById(definitionId).orElse(null);
    }

    @Override
    public ProcessInstanceEntity findProcessInstanceById(String instanceId) {
        return processInstanceRepository.findById(instanceId).orElse(null);
    }

    @Override
    public List<TaskEntity> findTasksByProcessInstanceId(String instanceId) {
        return taskRepository.findByProcessInstanceId(instanceId);
    }

    @Override
    public List<ProcessDefinitionEntity> findAllProcessDefinitions() {
        return processDefinitionRepository.findAll();
    }

    @Override
    public List<ProcessInstanceEntity> findAllProcessInstances() {
        return processInstanceRepository.findAll();
    }

    @Override
    public List<TaskEntity> findAllTasks() {
        return taskRepository.findAll();
    }
}