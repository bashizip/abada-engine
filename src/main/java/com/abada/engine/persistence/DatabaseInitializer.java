package com.abada.engine.persistence;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.util.BpmnUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

@Profile("dev")
@Component
public class DatabaseInitializer {

    private final AbadaEngine abadaEngine;
    private final PersistenceService persistenceService;
    private final Environment env;

    public DatabaseInitializer(AbadaEngine abadaEngine,
                               PersistenceService persistenceService,
                               Environment env) {
        this.abadaEngine = abadaEngine;
        this.persistenceService = persistenceService;
        this.env = env;
    }

    @PostConstruct
    public void initialize() {
        List<String> activeProfiles = List.of(env.getActiveProfiles());
        if (activeProfiles.contains("dev")) {
            System.out.println("[DEV] Auto-deploying sample process...");
            deploySampleProcessIfNotExists();
        }
    }

    private void deploySampleProcessIfNotExists() {
        boolean alreadyDeployed = persistenceService
                .findAllProcessDefinitions()
                .stream()
                .anyMatch(def -> def.getId().equals("simple-two-task"));

        if (alreadyDeployed) {
            System.out.println("[DEV] Sample process already deployed.");
            return;
        }

        try (InputStream stream = BpmnUtils.loadBpmnStream("simple-two-task.bpmn")) {
            abadaEngine.deploy(stream);
            System.out.println("[DEV] Sample process deployed.");
        } catch (Exception e) {
            System.err.println("[DEV] Failed to deploy sample process: " + e.getMessage());
        }
    }
}
