package com.abada.engine.util;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.TaskManager;
import com.abada.engine.core.model.TaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Sample data generator to preload the engine with process instances in various
 * states.
 * This component runs on application startup when the property
 * 'abada.generate-sample-data' is set to true.
 * 
 * Usage:
 * - Set environment variable: ABADA_GENERATE_SAMPLE_DATA=true
 * - Or add to application.properties: abada.generate-sample-data=true
 * 
 * The generator creates 6+ process instances using:
 * - recipe-cook.bpmn (4 instances)
 * - parallel-gateway-test.bpmn (2 instances)
 * 
 * Each instance is advanced to different states with various scenarios
 * including:
 * - Completed processes
 * - Failed processes
 * - In-progress processes at different stages
 * - Processes with different variable values
 */
@Component
@ConditionalOnProperty(name = "abada.generate-sample-data", havingValue = "true", matchIfMissing = false)
public class SampleDataGenerator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataGenerator.class);

    @Autowired
    private AbadaEngine abadaEngine;

    @Autowired
    private TaskManager taskManager;

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("Starting Sample Data Generation");
        log.info("========================================");

        // Deploy the BPMN processes
        deployProcesses();

        // Generate sample data for recipe-cook process
        generateRecipeCookData();

        // Generate sample data for parallel gateway process
        generateParallelGatewayData();

        log.info("========================================");
        log.info("Sample Data Generation Complete");
        log.info("========================================");
    }

    private void deployProcesses() throws Exception {
        log.info("Deploying BPMN processes...");

        // Deploy recipe-cook.bpmn (from main resources)
        try (InputStream recipeBpmn = loadBpmnStream("recipe-cook.bpmn")) {
            abadaEngine.deploy(recipeBpmn);
            log.info("✓ Deployed recipe-cook.bpmn");
        }

        // Deploy parallel-gateway-test.bpmn (from test resources)
        try (InputStream parallelBpmn = loadBpmnStream("parallel-gateway-test.bpmn")) {
            abadaEngine.deploy(parallelBpmn);
            log.info("✓ Deployed parallel-gateway-test.bpmn");
        }
    }

    /**
     * Loads a BPMN file from classpath resources.
     * Tries both main resources (bpmn/) and test resources (bpmn/).
     */
    private InputStream loadBpmnStream(String filename) {
        // Try main resources first
        InputStream stream = getClass().getClassLoader().getResourceAsStream("bpmn/" + filename);
        if (stream != null) {
            return stream;
        }

        // If not found, throw exception
        throw new IllegalArgumentException("BPMN file not found in classpath: bpmn/" + filename);
    }

    private void generateRecipeCookData() {
        log.info("\n--- Generating Recipe Cook Process Instances ---");

        // Scenario 1: Completed process - Alice chose a good recipe and it was cooked
        scenario1_CompletedRecipe();

        // Scenario 2: In-progress - Waiting at choose-recipe task
        scenario2_WaitingAtChooseRecipe();

        // Scenario 3: In-progress - Recipe chosen (goodOne=false), back to
        // choose-recipe
        scenario3_BackToChooseRecipe();

        // Scenario 4: In-progress - Recipe chosen (goodOne=true), waiting at
        // cook-recipe
        scenario4_WaitingAtCookRecipe();

        // Scenario 7: Failed Process - Started but then explicitly failed
        scenario7_FailedProcess();

        // Scenario 8: Failed Task - Task started but then failed
        scenario8_FailedTask();
    }

    private void scenario1_CompletedRecipe() {
        log.info("\nScenario 1: Completed Recipe Process");

        ProcessInstance pi = abadaEngine.startProcess("recipe-cook", "alice");
        log.info("  Started process: {}", pi.getId());

        // Alice chooses a recipe
        List<TaskInstance> tasks = taskManager.getTasksForProcessInstance(pi.getId());
        TaskInstance chooseTask = tasks.get(0);
        log.info("  Task available: {} - {}", chooseTask.getTaskDefinitionKey(), chooseTask.getName());

        // Alice claims and completes the task with goodOne=true
        abadaEngine.claim(chooseTask.getId(), "alice", List.of("customers"));
        log.info("  Alice claimed task: {}", chooseTask.getId());

        abadaEngine.completeTask(chooseTask.getId(), "alice", List.of("customers"),
                Map.of("goodOne", true, "recipeName", "Spaghetti Carbonara"));
        log.info("  Alice completed task with goodOne=true");

        // Bob cooks the recipe
        tasks = taskManager.getTasksForProcessInstance(pi.getId());
        TaskInstance cookTask = tasks.get(0);
        log.info("  Task available: {} - {}", cookTask.getTaskDefinitionKey(), cookTask.getName());

        abadaEngine.claim(cookTask.getId(), "bob", List.of("cuistos"));
        log.info("  Bob claimed task: {}", cookTask.getId());

        abadaEngine.completeTask(cookTask.getId(), "bob", List.of("cuistos"),
                Map.of("cookingTime", 30, "rating", 5));
        log.info("  Bob completed cooking task");

        log.info("  ✓ Process completed successfully");
    }

    private void scenario2_WaitingAtChooseRecipe() {
        log.info("\nScenario 2: Waiting at Choose Recipe");

        ProcessInstance pi = abadaEngine.startProcess("recipe-cook", "jeannot");
        log.info("  Started process: {}", pi.getId());
        log.info("  ✓ Process waiting at choose-recipe task (not yet claimed)");
    }

    private void scenario3_BackToChooseRecipe() {
        log.info("\nScenario 3: Back to Choose Recipe (goodOne=false)");

        ProcessInstance pi = abadaEngine.startProcess("recipe-cook", "black");
        log.info("  Started process: {}", pi.getId());

        // Black chooses a recipe but it's not good
        List<TaskInstance> tasks = taskManager.getTasksForProcessInstance(pi.getId());
        TaskInstance chooseTask = tasks.get(0);

        abadaEngine.claim(chooseTask.getId(), "black", List.of("customers"));
        log.info("  Black claimed task: {}", chooseTask.getId());

        abadaEngine.completeTask(chooseTask.getId(), "black", List.of("customers"),
                Map.of("goodOne", false, "recipeName", "Mystery Dish"));
        log.info("  Black completed task with goodOne=false");
        log.info("  ✓ Process looped back to choose-recipe task");
    }

    private void scenario4_WaitingAtCookRecipe() {
        log.info("\nScenario 4: Waiting at Cook Recipe");

        ProcessInstance pi = abadaEngine.startProcess("recipe-cook", "alice");
        log.info("  Started process: {}", pi.getId());

        // Alice chooses a good recipe
        List<TaskInstance> tasks = taskManager.getTasksForProcessInstance(pi.getId());
        TaskInstance chooseTask = tasks.get(0);

        abadaEngine.claim(chooseTask.getId(), "alice", List.of("customers"));
        abadaEngine.completeTask(chooseTask.getId(), "alice", List.of("customers"),
                Map.of("goodOne", true, "recipeName", "Chicken Tikka Masala"));
        log.info("  Alice completed choose-recipe with goodOne=true");

        // Now waiting for a cook to claim the task
        tasks = taskManager.getTasksForProcessInstance(pi.getId());
        TaskInstance cookTask = tasks.get(0);
        log.info("  ✓ Process waiting at cook-recipe task: {}", cookTask.getName());
    }

    private void scenario7_FailedProcess() {
        log.info("\nScenario 7: Explicitly Failed Process");

        ProcessInstance pi = abadaEngine.startProcess("recipe-cook", "system");
        log.info("  Started process: {}", pi.getId());

        // Simulate some error condition requiring process termination
        boolean failed = abadaEngine.failProcess(pi.getId());

        if (failed) {
            log.info("  ✓ Process explicitly failed successfully");
        } else {
            log.warn("  ! Failed to fail process");
        }
    }

    private void scenario8_FailedTask() {
        log.info("\nScenario 8: Explicitly Failed Task");

        ProcessInstance pi = abadaEngine.startProcess("recipe-cook", "alice");
        log.info("  Started process: {}", pi.getId());

        List<TaskInstance> tasks = taskManager.getTasksForProcessInstance(pi.getId());
        TaskInstance chooseTask = tasks.get(0);

        // Alice claims the task
        abadaEngine.claim(chooseTask.getId(), "alice", List.of("customers"));
        log.info("  Alice claimed task: {}", chooseTask.getId());

        // Something goes wrong during execution
        abadaEngine.failTask(chooseTask.getId());
        log.info("  ✓ Task explicitly marked as FAILED");
    }

    private void generateParallelGatewayData() {
        log.info("\n--- Generating Parallel Gateway Process Instances ---");

        // Scenario 5: Completed parallel process
        scenario5_CompletedParallelProcess();

        // Scenario 6: In-progress - One parallel branch completed
        scenario6_OneParallelBranchCompleted();
    }

    private void scenario5_CompletedParallelProcess() {
        log.info("\nScenario 5: Completed Parallel Gateway Process");

        ProcessInstance pi = abadaEngine.startProcess("ParallelGatewayProcess", "test-user");
        log.info("  Started process: {}", pi.getId());

        // Complete initial task
        List<TaskInstance> tasks = taskManager.getTasksForProcessInstance(pi.getId());
        TaskInstance initialTask = tasks.stream()
                .filter(t -> t.getTaskDefinitionKey().equals("InitialTask"))
                .findFirst()
                .orElseThrow();

        abadaEngine.claim(initialTask.getId(), "test-user", List.of("test-group"));
        abadaEngine.completeTask(initialTask.getId(), "test-user", List.of("test-group"),
                Map.of("initialData", "Setup complete"));
        log.info("  Completed InitialTask");

        // Complete both parallel tasks
        tasks = taskManager.getTasksForProcessInstance(pi.getId());
        log.info("  Parallel tasks created: {}", tasks.size());

        TaskInstance taskA = tasks.stream()
                .filter(t -> t.getTaskDefinitionKey().equals("TaskA"))
                .findFirst()
                .orElseThrow();

        TaskInstance taskB = tasks.stream()
                .filter(t -> t.getTaskDefinitionKey().equals("TaskB"))
                .findFirst()
                .orElseThrow();

        abadaEngine.claim(taskA.getId(), "test-user", List.of("test-group"));
        abadaEngine.completeTask(taskA.getId(), "test-user", List.of("test-group"),
                Map.of("taskAResult", "Branch A completed"));
        log.info("  Completed TaskA");

        abadaEngine.claim(taskB.getId(), "test-user", List.of("test-group"));
        abadaEngine.completeTask(taskB.getId(), "test-user", List.of("test-group"),
                Map.of("taskBResult", "Branch B completed"));
        log.info("  Completed TaskB");

        log.info("  ✓ Parallel process completed successfully");
    }

    private void scenario6_OneParallelBranchCompleted() {
        log.info("\nScenario 6: One Parallel Branch Completed");

        ProcessInstance pi = abadaEngine.startProcess("ParallelGatewayProcess", "test-user");
        log.info("  Started process: {}", pi.getId());

        // Complete initial task
        List<TaskInstance> tasks = taskManager.getTasksForProcessInstance(pi.getId());
        TaskInstance initialTask = tasks.stream()
                .filter(t -> t.getTaskDefinitionKey().equals("InitialTask"))
                .findFirst()
                .orElseThrow();

        abadaEngine.claim(initialTask.getId(), "test-user", List.of("test-group"));
        abadaEngine.completeTask(initialTask.getId(), "test-user", List.of("test-group"),
                Map.of("initialData", "Setup complete"));
        log.info("  Completed InitialTask");

        // Complete only TaskA, leave TaskB pending
        tasks = taskManager.getTasksForProcessInstance(pi.getId());
        TaskInstance taskA = tasks.stream()
                .filter(t -> t.getTaskDefinitionKey().equals("TaskA"))
                .findFirst()
                .orElseThrow();

        abadaEngine.claim(taskA.getId(), "test-user", List.of("test-group"));
        abadaEngine.completeTask(taskA.getId(), "test-user", List.of("test-group"),
                Map.of("taskAResult", "Branch A completed"));
        log.info("  Completed TaskA");

        log.info("  ✓ Process waiting at TaskB (parallel join not yet satisfied)");
    }
}
