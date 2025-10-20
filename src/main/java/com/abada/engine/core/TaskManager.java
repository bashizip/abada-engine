package com.abada.engine.core;

import com.abada.engine.core.exception.ProcessEngineException;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.core.model.TaskStatus;
import com.abada.engine.observability.EngineMetrics;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.annotation.SpanTag;
import io.micrometer.tracing.annotation.WithSpan;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TaskManager {

    private final Map<String, TaskInstance> tasks = new HashMap<>();
    private final EngineMetrics engineMetrics;
    private final Tracer tracer;

    @Autowired
    public TaskManager(EngineMetrics engineMetrics, Tracer tracer) {
        this.engineMetrics = engineMetrics;
        this.tracer = tracer;
    }

    @WithSpan("abada.task.create")
    public void createTask(@SpanTag("task.definition.key") String taskDefinitionKey, 
                          @SpanTag("task.name") String name, 
                          @SpanTag("process.instance.id") String processInstanceId,
                           String assignee, List<String> candidateUsers, List<String> candidateGroups) {

        Timer.Sample waitingTimeSample = engineMetrics.startTaskWaitingTimer();
        Span span = tracer.spanBuilder("abada.task.create").startSpan();
        
        try (var scope = span.makeCurrent()) {
            System.out.println("Creating task: " + name);
            TaskInstance task = new TaskInstance();
            task.setId(UUID.randomUUID().toString());
            task.setTaskDefinitionKey(taskDefinitionKey);
            task.setName(name);
            task.setProcessInstanceId(processInstanceId);
            task.setAssignee(assignee);
            task.setStartDate(Instant.now());

            if (assignee != null && !assignee.isEmpty()) {
                task.setStatus(TaskStatus.CLAIMED);
            } else {
                task.setStatus(TaskStatus.AVAILABLE);
            }

            if (candidateUsers != null) {
                task.getCandidateUsers().addAll(candidateUsers);
            }
            if (candidateGroups != null) {
                task.getCandidateGroups().addAll(candidateGroups);
            }

            tasks.put(task.getId(), task);
            
            span.setAttribute("task.id", task.getId());
            span.setAttribute("task.definition.key", taskDefinitionKey);
            span.setAttribute("task.name", name);
            span.setAttribute("process.instance.id", processInstanceId);
            span.setAttribute("task.status", task.getStatus().toString());
            span.setAttribute("task.assignee", assignee != null ? assignee : "");
            span.setAttribute("task.candidate.users.count", candidateUsers != null ? candidateUsers.size() : 0);
            span.setAttribute("task.candidate.groups.count", candidateGroups != null ? candidateGroups.size() : 0);
            
            engineMetrics.recordTaskCreated(taskDefinitionKey);
            
            // Store the waiting time sample for later use when task is claimed
            task.setWaitingTimeSample(waitingTimeSample);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }


    public Map<String, TaskInstance> getAllTasks() {
        return tasks;
    }

    @WithSpan("abada.task.claim")
    public void claimTask(@SpanTag("task.id") String taskId, 
                         @SpanTag("user.name") String user, 
                         List<String> userGroups) {
        Span span = tracer.spanBuilder("abada.task.claim").startSpan();
        
        try (var scope = span.makeCurrent()) {
            TaskInstance task = tasks.get(taskId);
            if (task == null) {
                throw new ProcessEngineException("Task not found: " + taskId);
            }
            if (task.getStatus() != TaskStatus.AVAILABLE) {
                throw new ProcessEngineException("Task is not available to be claimed. Current status: " + task.getStatus());
            }

            if (!isUserEligible(task, user, userGroups)) {
                throw new ProcessEngineException("User " + user + " is not eligible to claim task " + taskId);
            }

            task.setAssignee(user);
            task.setStatus(TaskStatus.CLAIMED);
            
            span.setAttribute("task.id", taskId);
            span.setAttribute("task.definition.key", task.getTaskDefinitionKey());
            span.setAttribute("task.name", task.getName());
            span.setAttribute("user.name", user);
            span.setAttribute("process.instance.id", task.getProcessInstanceId());
            
            engineMetrics.recordTaskClaimed(task.getTaskDefinitionKey());
            
            // Record waiting time and start processing time
            if (task.getWaitingTimeSample() != null) {
                engineMetrics.recordTaskWaitingTime(task.getWaitingTimeSample(), task.getTaskDefinitionKey());
            }
            task.setProcessingTimeSample(engineMetrics.startTaskProcessingTimer());
            
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    public void checkCanComplete(String taskId, String user, List<String> userGroups) {
        TaskInstance task = tasks.get(taskId);
        if (task == null) {
            throw new ProcessEngineException("Task not found: " + taskId);
        }
        if (task.isCompleted()) {
            throw new ProcessEngineException("Task is already completed.");
        }

        boolean isAssignee = user.equals(task.getAssignee());
        boolean isEligibleAndAvailable = task.getStatus() == TaskStatus.AVAILABLE && isUserEligible(task, user, userGroups);

        if (!isAssignee && !isEligibleAndAvailable) {
            throw new ProcessEngineException("User " + user + " is not authorized to complete task " + taskId);
        }
    }

    @WithSpan("abada.task.complete")
    public void completeTask(@SpanTag("task.id") String taskId) {
        Span span = tracer.spanBuilder("abada.task.complete").startSpan();
        
        try (var scope = span.makeCurrent()) {
            TaskInstance task = tasks.get(taskId);
            if (task != null) {
                task.setStatus(TaskStatus.COMPLETED);
                task.setEndDate(Instant.now());
                
                span.setAttribute("task.id", taskId);
                span.setAttribute("task.definition.key", task.getTaskDefinitionKey());
                span.setAttribute("task.name", task.getName());
                span.setAttribute("user.name", task.getAssignee());
                span.setAttribute("process.instance.id", task.getProcessInstanceId());
                
                engineMetrics.recordTaskCompleted(task.getTaskDefinitionKey());
                
                // Record processing time
                if (task.getProcessingTimeSample() != null) {
                    engineMetrics.recordTaskProcessingTime(task.getProcessingTimeSample(), task.getTaskDefinitionKey());
                }
            }
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @WithSpan("abada.task.fail")
    public void failTask(@SpanTag("task.id") String taskId) {
        Span span = tracer.spanBuilder("abada.task.fail").startSpan();
        
        try (var scope = span.makeCurrent()) {
            TaskInstance task = tasks.get(taskId);
            if (task == null || task.isCompleted()) { // Can't fail a non-existent or completed task
                throw new ProcessEngineException("Task not found or is already completed: " + taskId);
            }
            task.setStatus(TaskStatus.FAILED);
            task.setEndDate(Instant.now());
            
            span.setAttribute("task.id", taskId);
            span.setAttribute("task.definition.key", task.getTaskDefinitionKey());
            span.setAttribute("task.name", task.getName());
            span.setAttribute("user.name", task.getAssignee());
            span.setAttribute("process.instance.id", task.getProcessInstanceId());
            
            engineMetrics.recordTaskFailed(task.getTaskDefinitionKey());
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    public List<TaskInstance> getVisibleTasksForUser(String user, List<String> groups) {
        return getVisibleTasksForUser(user, groups, null);
    }

    public List<TaskInstance> getVisibleTasksForUser(String user, List<String> groups, TaskStatus status) {
        System.out.println("All tasks: " + tasks);
        Stream<TaskInstance> stream = tasks.values().stream()
                .filter(task -> task.getStatus() != TaskStatus.COMPLETED && task.getStatus() != TaskStatus.FAILED)
                .filter(task -> isUserEligible(task, user, groups));

        if (status != null) {
            stream = stream.filter(task -> task.getStatus() == status);
        }

        List<TaskInstance> result = stream.toList();
        System.out.println("Visible tasks for user " + user + " in groups " + groups + " with status " + status + ": " + result.toString());
        return result;
    }

    public Optional<TaskInstance> getTask(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public Optional<TaskInstance> getTaskByDefinitionKey(String taskDefinitionKey, String processInstanceId) {
        return tasks.values().stream()
                .filter(task -> taskDefinitionKey.equals(task.getTaskDefinitionKey()) &&
                        processInstanceId.equals(task.getProcessInstanceId()))
                .findFirst();
    }

    public List<TaskInstance> getTasksForProcessInstance(String processInstanceId) {
        return tasks.values().stream()
                .filter(t -> t.getProcessInstanceId().equals(processInstanceId) && t.getStatus() != TaskStatus.COMPLETED)
                .collect(Collectors.toList());
    }

    private boolean isUserEligible(TaskInstance task, String user, List<String> groups) {
        System.out.println("Checking eligibility for task: " + task.getName());
        System.out.println("Task candidate users: " + task.getCandidateUsers());
        System.out.println("Task candidate groups: " + task.getCandidateGroups());
        System.out.println("User: " + user);
        System.out.println("User groups: " + groups);
        if (task.getAssignee() != null) {
            return task.getAssignee().equals(user); // direct assignee match
        }
        // If unassigned, check candidate users and groups
        return task.getCandidateUsers().contains(user) ||
                (groups != null && groups.stream().anyMatch(task.getCandidateGroups()::contains));
    }


    public void addTask(TaskInstance taskInstance) {
        tasks.put(taskInstance.getId(), taskInstance);
    }

    public void clearTasks() {
        tasks.clear();
    }

}
