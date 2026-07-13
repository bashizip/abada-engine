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

            // Poison both legacy caches after claiming. Completion must ignore these
            // objects and reconstruct authoritative command state from PostgreSQL.
            recoveredTasks.getFirst().setStatus(TaskStatus.COMPLETED);
            restartedEngine.getProcessInstanceById(processInstanceId)
                    .putAllVariables(Map.of("cacheOnly", "must-not-be-persisted"));

            restartedEngine.completeTask(taskId, "alice", List.of("operators"),
                    Map.of("approved", true));

            ProcessInstance completed = restartedEngine.getProcessInstanceById(processInstanceId);
            assertThat(completed.isCompleted()).isTrue();
            assertThat(completed.getVariables())
                    .containsEntry("requestId", "request-42")
                    .containsEntry("approved", true)
                    .doesNotContainKey("cacheOnly");

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
