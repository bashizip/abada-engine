package com.abada.engine.persistence.impl;

import com.abada.engine.persistence.PersistenceService;
import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.entity.ProcessInstanceEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import com.abada.engine.persistence.repository.ProcessDefinitionRepository;
import com.abada.engine.persistence.repository.ProcessInstanceRepository;
import com.abada.engine.persistence.repository.TaskRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
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
    @Transactional
    public void saveOrUpdateProcessInstance(ProcessInstanceEntity instance) {
        if (instance == null) {
            throw new IllegalArgumentException("ProcessInstance cannot be null");
        }
        processInstanceRepository.save(instance);
    }
    @Override
    public void saveProcessDefinition(ProcessDefinitionEntity definition) {
        processDefinitionRepository.save(definition);
    }

    @Override
    @Transactional
    public void saveProcessInstance(ProcessInstanceEntity instance) {
          saveOrUpdateProcessInstance(instance);
    }

    @Override
    public void saveTask(TaskEntity task) {
        if (task == null) {
            throw new IllegalArgumentException("task cannot be null");
        }
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