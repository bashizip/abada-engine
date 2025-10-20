package com.abada.engine.core;

import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.core.model.TaskStatus;
import com.abada.engine.observability.EngineMetrics;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TaskManagerTest {

    @Mock
    private EngineMetrics engineMetrics;
    
    @Mock
    private Tracer tracer;
    
    @Mock
    private Span span;
    
    @Mock
    private Timer.Sample timerSample;
    
    @Mock
    private io.opentelemetry.api.trace.SpanBuilder spanBuilder;
    
    private TaskManager taskManager;

    @BeforeEach
    void setUp() {
        // Setup timer samples
        when(engineMetrics.startTaskWaitingTimer()).thenReturn(timerSample);
        when(engineMetrics.startTaskProcessingTimer()).thenReturn(timerSample);
        
        // Setup span creation
        when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        when(spanBuilder.startSpan()).thenReturn(span);
        when(span.makeCurrent()).thenReturn(() -> {});
        
        taskManager = new TaskManager(engineMetrics, tracer);
    }

    @Test
    void testCreateAndRetrieveTask() {
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask(
                "approveTask", "Approve Request", processInstanceId,
                null,
                List.of("user1"),
                List.of("group1")
        );

        List<TaskInstance> visibleTasks = taskManager.getVisibleTasksForUser("user1", List.of("group1"));
        assertThat(visibleTasks).hasSize(1);

        TaskInstance task = visibleTasks.get(0);
        assertThat(task.getTaskDefinitionKey()).isEqualTo("approveTask");
        assertThat(task.getProcessInstanceId()).isEqualTo(processInstanceId);
        assertThat(task.getAssignee()).isNull();
        assertThat(task.isCompleted()).isFalse();
    }

    @Test
    void testClaimTask() {
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask(
                "reviewTask", "Review Document", processInstanceId,
                null,
                List.of("user2"),
                List.of("group2")
        );

        List<TaskInstance> visible = taskManager.getVisibleTasksForUser("user2", List.of("group2"));
        assertThat(visible).hasSize(1);

        TaskInstance task = visible.get(0);

        taskManager.claimTask(task.getId(), "user2", List.of("group2"));

        Assertions.assertEquals(TaskStatus.CLAIMED, task.getStatus());
        assertThat(task.getAssignee()).isEqualTo("user2");
        assertThat(task.isCompleted()).isFalse();
    }

    @Test
    void testCompleteTask() {
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask(
                "signOffTask", "Final Sign Off", processInstanceId,
                "user3", // already assigned
                null,
                null
        );


        TaskInstance task = taskManager.getTaskByDefinitionKey("signOffTask", processInstanceId).orElseThrow();

        assertThat(task.getAssignee()).isEqualTo("user3");

        taskManager.completeTask(task.getId());
        assertThat(task.isCompleted()).isTrue();

        // Once isCompleted, it should no longer be visible
        List<TaskInstance> postCompleteVisible = taskManager.getVisibleTasksForUser("user3", List.of("group3"));
        assertThat(postCompleteVisible).isEmpty();
    }

    @Test
    void testGetTaskByDefinitionKey() {
        String processInstanceId = UUID.randomUUID().toString();

        taskManager.createTask(
                "validateInvoice", "Validate Invoice", processInstanceId,
                null,
                List.of("user4"),
                List.of("group4")
        );

        Optional<TaskInstance> retrieved = taskManager.getTaskByDefinitionKey("validateInvoice", processInstanceId);
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Validate Invoice");
        assertThat(retrieved.get().isCompleted()).isFalse();
    }
}
