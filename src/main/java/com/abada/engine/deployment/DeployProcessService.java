package com.abada.engine.deployment;

import com.abada.engine.persistence.PersistenceService;
import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.runtime.DeployedProcessDefinition;
import com.abada.engine.runtime.ProcessRegistry;
import com.abada.engine.core.model.ParsedProcessDefinition;
import org.springframework.stereotype.Service;

@Service
public class DeployProcessService {

    private final PersistenceService persistenceService;
    private final ProcessRegistry processRegistry;

    public DeployProcessService(PersistenceService persistenceService,
                                ProcessRegistry processRegistry) {
        this.persistenceService = persistenceService;
        this.processRegistry = processRegistry;
    }

    public void deploy(ParsedProcessDefinition parsedDefinition) {
        // 1. Persist the ProcessDefinition
        ProcessDefinitionEntity entity = mapToEntity(parsedDefinition);
        persistenceService.saveProcessDefinition(entity);

        // 2. Register it in runtime
        DeployedProcessDefinition deployed = mapToDeployed(parsedDefinition);
        processRegistry.register(deployed);

        System.out.println("[AbadaEngine] ProcessDefinition deployed and persisted: " + parsedDefinition.getId());
    }

    private ProcessDefinitionEntity mapToEntity(ParsedProcessDefinition parsed) {
        ProcessDefinitionEntity entity = new ProcessDefinitionEntity();
        entity.setId(parsed.getId());
        entity.setName(parsed.getName());
        entity.setBpmnXml(parsed.getRawXml());
        return entity;
    }

    private DeployedProcessDefinition mapToDeployed(ParsedProcessDefinition parsed) {
        return new DeployedProcessDefinition(parsed.getId(), parsed.getName(), parsed.getRawXml());
    }
}
