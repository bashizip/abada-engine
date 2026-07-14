package com.abada.engine.persistence;

import com.abada.engine.AbadaEngineApplication;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.exception.ProcessEngineException;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.core.model.TaskStatus;
import com.abada.engine.persistence.entity.ActivityHistoryEntity;
import com.abada.engine.persistence.entity.TaskEntity;
import com.abada.engine.persistence.repository.ActivityHistoryRepository;
import com.abada.engine.persistence.repository.ProcessInstanceRepository;
import com.abada.engine.persistence.repository.TaskRepository;
import com.abada.engine.util.DatabaseTestHelper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.support.TestPropertySourceUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PostgresRestartRecoveryTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("abada_restart_recovery")
                    .withUsername("abada")
                    .withPassword("abada");

    @Test
    void pagesRuntimeQueriesInPostgresWithoutDuplicateRows() {
        try (ConfigurableApplicationContext context = startApplication()) {
            context.getBean(DatabaseTestHelper.class).cleanup();
            AbadaEngine engine = context.getBean(AbadaEngine.class);
            deployRestartProcess(engine);

            for (int instance = 0; instance < 5; instance++) {
                engine.startProcess("postgres-restart-recovery", "alice", Map.of("sequence", instance));
            }

            Sort taskOrder = Sort.by("startDate").ascending().and(Sort.by("id").ascending());
            Page<TaskInstance> firstTasks = engine.getTaskManager().getVisibleTasksForUser(
                    "alice", List.of("operators"), null, PageRequest.of(0, 2, taskOrder));
            Page<TaskInstance> secondTasks = engine.getTaskManager().getVisibleTasksForUser(
                    "alice", List.of("operators"), null, PageRequest.of(1, 2, taskOrder));

            assertThat(firstTasks.getTotalElements()).isEqualTo(5);
            assertThat(firstTasks.getContent()).hasSize(2);
            assertThat(secondTasks.getContent()).hasSize(2);
            assertThat(secondTasks.getContent())
                    .extracting(TaskInstance::getId)
                    .doesNotContainAnyElementsOf(firstTasks.map(TaskInstance::getId).getContent());

            Sort instanceOrder = Sort.by("startDate").descending().and(Sort.by("id").ascending());
            Page<ProcessInstance> firstInstances = engine.getProcessInstances(
                    PageRequest.of(0, 3, instanceOrder));
            Page<ProcessInstance> secondInstances = engine.getProcessInstances(
                    PageRequest.of(1, 3, instanceOrder));

            assertThat(firstInstances.getTotalElements()).isEqualTo(5);
            assertThat(firstInstances.getContent()).hasSize(3);
            assertThat(secondInstances.getContent()).hasSize(2);
            assertThat(secondInstances.getContent())
                    .extracting(ProcessInstance::getId)
                    .doesNotContainAnyElementsOf(firstInstances.map(ProcessInstance::getId).getContent());
        }
    }

    @Test
    void completesPersistedUserTaskAfterFullApplicationRestart() {
        String processInstanceId;
        String taskId;

        try (ConfigurableApplicationContext firstContext = startApplication()) {
            firstContext.getBean(DatabaseTestHelper.class).cleanup();
            AbadaEngine engine = firstContext.getBean(AbadaEngine.class);

            try (InputStream bpmn = getClass().getResourceAsStream(
                    "/bpmn/postgres-restart-recovery.bpmn")) {
                assertThat(bpmn).as("restart-recovery BPMN resource").isNotNull();
                engine.deploy(bpmn);
            } catch (Exception exception) {
                throw new AssertionError("Could not deploy restart-recovery BPMN", exception);
            }

            ProcessInstance instance = engine.startProcess(
                    "postgres-restart-recovery", "alice", Map.of("requestId", "request-42"));
            processInstanceId = instance.getId();

            List<TaskInstance> tasks = engine.getTaskManager()
                    .getVisibleTasksForUser("alice", List.of("operators"));
            assertThat(tasks).hasSize(1);
            taskId = tasks.getFirst().getId();

            assertThat(firstContext.getBean(ProcessInstanceRepository.class).findById(processInstanceId))
                    .isPresent()
                    .get()
                    .satisfies(entity -> {
                        assertThat(entity.getVariablesJson()).contains("request-42");
                        assertThat(entity.getActiveTokensJson()).contains("approval");
                    });
            assertThat(firstContext.getBean(TaskRepository.class).findById(taskId))
                    .isPresent()
                    .get()
                    .extracting(task -> task.getStatus())
                    .isEqualTo(TaskStatus.AVAILABLE);
        }

        try (ConfigurableApplicationContext restartedContext = startApplication()) {
            AbadaEngine restartedEngine = restartedContext.getBean(AbadaEngine.class);

            List<TaskInstance> recoveredTasks = restartedEngine.getTaskManager()
                    .getVisibleTasksForUser("alice", List.of("operators"));
            assertThat(recoveredTasks)
                    .extracting(TaskInstance::getId)
                    .containsExactly(taskId);

            restartedEngine.claim(taskId, "alice", List.of("operators"));

            // Poison detached query snapshots. A later command must reconstruct its
            // authoritative state from PostgreSQL rather than observing these changes.
            recoveredTasks.getFirst().setStatus(TaskStatus.COMPLETED);
            ProcessInstance detachedInstance = restartedEngine.getProcessInstanceById(processInstanceId);
            detachedInstance.putAllVariables(Map.of("memoryOnly", "must-not-be-persisted"));
            assertThat(restartedEngine.getProcessInstanceById(processInstanceId).getVariables())
                    .doesNotContainKey("memoryOnly");

            restartedEngine.completeTask(taskId, "alice", List.of("operators"),
                    Map.of("approved", true));

            ProcessInstance completed = restartedEngine.getProcessInstanceById(processInstanceId);
            assertThat(completed.isCompleted()).isTrue();
            assertThat(completed.getVariables())
                    .containsEntry("requestId", "request-42")
                    .containsEntry("approved", true)
                    .doesNotContainKey("memoryOnly");

            assertThat(restartedContext.getBean(TaskRepository.class).findById(taskId))
                    .isPresent()
                    .get()
                    .extracting(task -> task.getStatus())
                    .isEqualTo(TaskStatus.COMPLETED);

            List<ActivityHistoryEntity> history = restartedContext
                    .getBean(ActivityHistoryRepository.class)
                    .findByProcessInstanceIdOrderByOccurredAtAsc(processInstanceId);
            assertThat(history)
                    .extracting(ActivityHistoryEntity::getEventType)
                    .containsExactly("PROCESS_STARTED", "TASK_CLAIMED", "TASK_COMPLETED");
        }
    }

    @Test
    void serializesConcurrentClaimAndFailureAcrossTwoEngineReplicas() throws Exception {
        try (ConfigurableApplicationContext firstContext = startApplication()) {
            firstContext.getBean(DatabaseTestHelper.class).cleanup();
            AbadaEngine firstEngine = firstContext.getBean(AbadaEngine.class);
            deployRestartProcess(firstEngine);

            ProcessInstance instance = firstEngine.startProcess(
                    "postgres-restart-recovery", "alice", Map.of());
            String taskId = firstEngine.getTaskManager()
                    .getVisibleTasksForUser("alice", List.of("operators"))
                    .getFirst()
                    .getId();

            try (ConfigurableApplicationContext secondContext = startApplication()) {
                AbadaEngine secondEngine = secondContext.getBean(AbadaEngine.class);

                assertThat(runConcurrentAttempts(
                        claimAttempt(firstEngine, taskId),
                        claimAttempt(secondEngine, taskId)))
                        .containsExactlyInAnyOrder(true, false);
                assertThat(firstContext.getBean(TaskRepository.class).findById(taskId))
                        .isPresent()
                        .get()
                        .extracting(TaskEntity::getStatus)
                        .isEqualTo(TaskStatus.CLAIMED);

                assertThat(runConcurrentAttempts(
                        failureAttempt(firstEngine, taskId),
                        failureAttempt(secondEngine, taskId)))
                        .containsExactlyInAnyOrder(true, false);
                assertThat(firstContext.getBean(TaskRepository.class).findById(taskId))
                        .isPresent()
                        .get()
                        .extracting(TaskEntity::getStatus)
                        .isEqualTo(TaskStatus.FAILED);

                List<ActivityHistoryEntity> history = firstContext
                        .getBean(ActivityHistoryRepository.class)
                        .findByProcessInstanceIdOrderByOccurredAtAsc(instance.getId());
                assertThat(history)
                        .filteredOn(event -> "TASK_CLAIMED".equals(event.getEventType()))
                        .hasSize(1);
                assertThat(history)
                        .filteredOn(event -> "TASK_FAILED".equals(event.getEventType()))
                        .hasSize(1);
            }
        }
    }

    @Test
    void serializesConcurrentCompletionAcrossTwoEngineReplicas() throws Exception {
        try (ConfigurableApplicationContext firstContext = startApplication()) {
            firstContext.getBean(DatabaseTestHelper.class).cleanup();
            AbadaEngine firstEngine = firstContext.getBean(AbadaEngine.class);
            deployRestartProcess(firstEngine);

            ProcessInstance instance = firstEngine.startProcess(
                    "postgres-restart-recovery", "alice", Map.of());
            String processInstanceId = instance.getId();
            String taskId = firstEngine.getTaskManager()
                    .getVisibleTasksForUser("alice", List.of("operators"))
                    .getFirst()
                    .getId();

            try (ConfigurableApplicationContext secondContext = startApplication()) {
                AbadaEngine secondEngine = secondContext.getBean(AbadaEngine.class);
                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);

                try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                    Future<Boolean> firstResult = executor.submit(
                            completionAttempt(firstEngine, taskId, ready, start));
                    Future<Boolean> secondResult = executor.submit(
                            completionAttempt(secondEngine, taskId, ready, start));

                    assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
                    start.countDown();

                    assertThat(List.of(
                            firstResult.get(10, TimeUnit.SECONDS),
                            secondResult.get(10, TimeUnit.SECONDS)))
                            .containsExactlyInAnyOrder(true, false);
                }

                assertThat(firstContext.getBean(TaskRepository.class).findById(taskId))
                        .isPresent()
                        .get()
                        .extracting(TaskEntity::getStatus)
                        .isEqualTo(TaskStatus.COMPLETED);
                assertThat(firstContext.getBean(ProcessInstanceRepository.class)
                        .findById(processInstanceId))
                        .isPresent()
                        .get()
                        .satisfies(entity -> assertThat(entity.getStatus().name()).isEqualTo("COMPLETED"));

                List<ActivityHistoryEntity> history = firstContext
                        .getBean(ActivityHistoryRepository.class)
                        .findByProcessInstanceIdOrderByOccurredAtAsc(processInstanceId);
                assertThat(history)
                        .filteredOn(event -> "TASK_COMPLETED".equals(event.getEventType()))
                        .hasSize(1);
            }
        }
    }

    @Test
    void preservesConcurrentVariableUpdatesAcrossTwoEngineReplicas() throws Exception {
        try (ConfigurableApplicationContext firstContext = startApplication()) {
            firstContext.getBean(DatabaseTestHelper.class).cleanup();
            AbadaEngine firstEngine = firstContext.getBean(AbadaEngine.class);
            deployRestartProcess(firstEngine);

            ProcessInstance instance = firstEngine.startProcess(
                    "postgres-restart-recovery", "alice", Map.of("initial", true));

            try (ConfigurableApplicationContext secondContext = startApplication()) {
                AbadaEngine secondEngine = secondContext.getBean(AbadaEngine.class);

                assertThat(runConcurrentAttempts(
                        variableUpdateAttempt(firstEngine, instance.getId(), "fromFirstReplica", 1),
                        variableUpdateAttempt(secondEngine, instance.getId(), "fromSecondReplica", 2)))
                        .containsExactly(true, true);

                assertThat(firstEngine.getProcessInstanceById(instance.getId()).getVariables())
                        .containsEntry("initial", true)
                        .containsEntry("fromFirstReplica", 1)
                        .containsEntry("fromSecondReplica", 2);

                List<ActivityHistoryEntity> history = firstContext
                        .getBean(ActivityHistoryRepository.class)
                        .findByProcessInstanceIdOrderByOccurredAtAsc(instance.getId());
                assertThat(history)
                        .filteredOn(event -> "VARIABLES_UPDATED".equals(event.getEventType()))
                        .hasSize(2);
            }
        }
    }

    private Callable<Boolean> completionAttempt(AbadaEngine engine, String taskId,
            CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            try {
                engine.completeTask(taskId, "alice", List.of("operators"), Map.of());
                return true;
            } catch (ProcessEngineException alreadyCompleted) {
                return false;
            }
        };
    }

    private Callable<Boolean> claimAttempt(AbadaEngine engine, String taskId) {
        return () -> {
            try {
                engine.claim(taskId, "alice", List.of("operators"));
                return true;
            } catch (ProcessEngineException alreadyClaimed) {
                return false;
            }
        };
    }

    private Callable<Boolean> failureAttempt(AbadaEngine engine, String taskId) {
        return () -> {
            try {
                engine.failTask(taskId);
                return true;
            } catch (ProcessEngineException alreadyFailed) {
                return false;
            }
        };
    }

    private Callable<Boolean> variableUpdateAttempt(
            AbadaEngine engine, String processInstanceId, String variableName, Object value) {
        return () -> {
            engine.updateProcessVariables(processInstanceId, Map.of(variableName, value));
            return true;
        };
    }

    private List<Boolean> runConcurrentAttempts(
            Callable<Boolean> firstAttempt, Callable<Boolean> secondAttempt) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Boolean> gatedFirst = gatedAttempt(firstAttempt, ready, start);
        Callable<Boolean> gatedSecond = gatedAttempt(secondAttempt, ready, start);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Boolean> firstResult = executor.submit(gatedFirst);
            Future<Boolean> secondResult = executor.submit(gatedSecond);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(
                    firstResult.get(10, TimeUnit.SECONDS),
                    secondResult.get(10, TimeUnit.SECONDS));
        }
    }

    private Callable<Boolean> gatedAttempt(Callable<Boolean> attempt,
            CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            return attempt.call();
        };
    }

    private void deployRestartProcess(AbadaEngine engine) {
        try (InputStream bpmn = getClass().getResourceAsStream(
                "/bpmn/postgres-restart-recovery.bpmn")) {
            assertThat(bpmn).as("restart-recovery BPMN resource").isNotNull();
            engine.deploy(bpmn);
        } catch (Exception exception) {
            throw new AssertionError("Could not deploy restart-recovery BPMN", exception);
        }
    }

    private ConfigurableApplicationContext startApplication() {
        return new SpringApplicationBuilder(AbadaEngineApplication.class)
                .web(WebApplicationType.SERVLET)
                .initializers(context -> TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                        context,
                        "server.port=0",
                        "spring.datasource.url=" + POSTGRES.getJdbcUrl(),
                        "spring.datasource.username=" + POSTGRES.getUsername(),
                        "spring.datasource.password=" + POSTGRES.getPassword(),
                        "spring.datasource.driver-class-name=org.postgresql.Driver",
                        "spring.datasource.hikari.maximum-pool-size=3",
                        "spring.datasource.hikari.minimum-idle=1",
                        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.jpa.open-in-view=false",
                        "spring.flyway.enabled=true",
                        "spring.task.scheduling.enabled=false",
                        "abada.security.mode=disabled",
                        "otel.sdk.disabled=true",
                        "management.tracing.enabled=false",
                        "management.otlp.metrics.export.enabled=false"))
                .run("--spring.profiles.active=test");
    }
}
