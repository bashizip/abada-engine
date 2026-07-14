package com.abada.engine.persistence;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import com.abada.engine.persistence.repository.ProcessDefinitionRepository;
import com.abada.engine.persistence.repository.ProcessInstanceRepository;
import com.abada.engine.util.DatabaseTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ProcessDefinitionCacheTest {

    @Autowired
    private AbadaEngine engine;

    @Autowired
    private DatabaseTestHelper databaseTestHelper;

    @Autowired
    private ProcessDefinitionRepository definitionRepository;

    @Autowired
    private ProcessInstanceRepository instanceRepository;

    @BeforeEach
    void cleanState() {
        databaseTestHelper.cleanup();
        engine.clearMemory();
    }

    @Test
    void pinsInstancesToImmutableDeploymentsAndRecoversAfterCacheLoss() throws Exception {
        String firstXml;
        try (InputStream resource = getClass().getResourceAsStream("/bpmn/recipe-cook.bpmn")) {
            assertThat(resource).isNotNull();
            firstXml = new String(resource.readAllBytes(), StandardCharsets.UTF_8);
        }

        ProcessDefinitionEntity firstDeployment = deploy(firstXml);
        ProcessInstance firstInstance = engine.startProcess("recipe-cook");

        String secondXml = firstXml
                .replace("Choose Recipe", "Choose Recipe v2")
                .replace("Cook Recipe", "Cook Recipe v2");
        ProcessDefinitionEntity secondDeployment = deploy(secondXml);
        ProcessDefinitionEntity duplicateDeployment = deploy(secondXml);
        ProcessInstance secondInstance = engine.startProcess("recipe-cook");

        assertThat(firstDeployment.getVersion()).isEqualTo(1);
        assertThat(secondDeployment.getVersion()).isEqualTo(2);
        assertThat(secondDeployment.getDeploymentId()).isNotEqualTo(firstDeployment.getDeploymentId());
        assertThat(duplicateDeployment.getDeploymentId()).isEqualTo(secondDeployment.getDeploymentId());
        assertThat(definitionRepository.count()).isEqualTo(2);

        assertThat(firstInstance.getProcessDefinitionDeploymentId())
                .isEqualTo(firstDeployment.getDeploymentId());
        assertThat(secondInstance.getProcessDefinitionDeploymentId())
                .isEqualTo(secondDeployment.getDeploymentId());

        engine.clearMemory();

        ProcessInstance reloadedFirst = engine.getProcessInstanceById(firstInstance.getId());
        ProcessInstance reloadedSecond = engine.getProcessInstanceById(secondInstance.getId());

        assertThat(reloadedFirst.getDefinition().getTaskName("choose-recipe"))
                .isEqualTo("Choose Recipe");
        assertThat(reloadedSecond.getDefinition().getTaskName("choose-recipe"))
                .isEqualTo("Choose Recipe v2");
        assertThat(engine.getParsedProcessDefinition("recipe-cook").getTaskName("choose-recipe"))
                .isEqualTo("Choose Recipe v2");

        completeInitialTask(firstInstance.getId());
        completeInitialTask(secondInstance.getId());

        assertThat(engine.getTaskManager().getTasksForProcessInstance(firstInstance.getId()))
                .singleElement()
                .extracting(TaskInstance::getName)
                .isEqualTo("Cook Recipe");
        assertThat(engine.getTaskManager().getTasksForProcessInstance(secondInstance.getId()))
                .singleElement()
                .extracting(TaskInstance::getName)
                .isEqualTo("Cook Recipe v2");

        assertThat(instanceRepository.findById(firstInstance.getId()))
                .isPresent()
                .get()
                .extracting(entity -> entity.getProcessDefinitionDeploymentId())
                .isEqualTo(firstDeployment.getDeploymentId());
        assertThat(instanceRepository.findById(secondInstance.getId()))
                .isPresent()
                .get()
                .extracting(entity -> entity.getProcessDefinitionDeploymentId())
                .isEqualTo(secondDeployment.getDeploymentId());
    }

    private ProcessDefinitionEntity deploy(String xml) {
        return engine.deploy(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private void completeInitialTask(String processInstanceId) {
        List<TaskInstance> tasks = engine.getTaskManager().getTasksForProcessInstance(processInstanceId);
        assertThat(tasks).singleElement();
        engine.completeTask(tasks.getFirst().getId(), "alice", List.of("customers"), Map.of("goodOne", true));
    }
}
