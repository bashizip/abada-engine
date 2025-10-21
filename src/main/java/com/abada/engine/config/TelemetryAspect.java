package com.abada.engine.config;

import com.abada.engine.core.model.ProcessInstance;
import com.abada.engine.core.model.TaskInstance;
import io.micrometer.core.instrument.Timer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Aspect
@Component
public class TelemetryAspect {
    private static final Logger log = LoggerFactory.getLogger(TelemetryAspect.class);
    
    private final EngineMetrics metrics;
    private final Tracer tracer;
    private final Map<String, Timer.Sample> processTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer.Sample> taskWaitingTimers = new ConcurrentHashMap<>();
    private final Map<String, Timer.Sample> taskProcessingTimers = new ConcurrentHashMap<>();

    public TelemetryAspect(TelemetryConfig.EngineMetrics metrics, Tracer tracer) {
        this.metrics = metrics;
        this.tracer = tracer;
    }

    @Around("execution(* com.abada.engine.core.AbadaEngine.startProcess(..))")
    public Object traceProcessStart(ProceedingJoinPoint joinPoint) throws Throwable {
        String definitionId = (String) joinPoint.getArgs()[0];
        
        Span span = tracer.spanBuilder("abada.process.start")
            .setAttribute("process.definition.id", definitionId)
            .startSpan();
        
        try (var scope = span.makeCurrent()) {
            metrics.recordProcessStarted(definitionId);
            Timer.Sample timer = metrics.startProcessTimer();
            processTimers.put(definitionId, timer);
            
            Object result = joinPoint.proceed();
            
            if (result instanceof ProcessInstance process) {
                span.setAttribute("process.instance.id", process.getId());
            }
            
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @Around("execution(* com.abada.engine.core.AbadaEngine.completeProcess(..))")
    public Object traceProcessCompletion(ProceedingJoinPoint joinPoint) throws Throwable {
        ProcessInstance process = (ProcessInstance) joinPoint.getArgs()[0];
        String definitionId = process.getDefinitionId();
        String instanceId = process.getId();
        
        Span span = tracer.spanBuilder("abada.process.complete")
            .setAttribute("process.definition.id", definitionId)
            .setAttribute("process.instance.id", instanceId)
            .startSpan();
        
        try (var scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            
            Timer.Sample timer = processTimers.remove(definitionId);
            if (timer != null) {
                metrics.recordProcessCompleted(definitionId, timer);
            }
            
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            Timer.Sample timer = processTimers.remove(definitionId);
            if (timer != null) {
                metrics.recordProcessFailed(definitionId, timer);
            }
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @Around("execution(* com.abada.engine.core.TaskManager.createTask(..))")
    public Object traceTaskCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        TaskInstance task = (TaskInstance) joinPoint.getArgs()[0];
        String taskKey = task.getDefinitionKey();
        
        Span span = tracer.spanBuilder("abada.task.create")
            .setAttribute("task.definition.key", taskKey)
            .setAttribute("process.instance.id", task.getProcessInstanceId())
            .startSpan();
        
        try (var scope = span.makeCurrent()) {
            metrics.recordTaskCreated(taskKey);
            Timer.Sample timer = metrics.startTaskWaitingTimer();
            taskWaitingTimers.put(task.getId(), timer);
            
            Object result = joinPoint.proceed();
            
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @Around("execution(* com.abada.engine.core.TaskManager.claimTask(..))")
    public Object traceTaskClaim(ProceedingJoinPoint joinPoint) throws Throwable {
        String taskId = (String) joinPoint.getArgs()[0];
        String userId = (String) joinPoint.getArgs()[1];
        
        Span span = tracer.spanBuilder("abada.task.claim")
            .setAttribute("task.id", taskId)
            .setAttribute("user.id", userId)
            .startSpan();
        
        try (var scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            
            if (result instanceof TaskInstance task) {
                Timer.Sample waitingTimer = taskWaitingTimers.remove(taskId);
                Timer.Sample processingTimer = metrics.startTaskProcessingTimer();
                
                metrics.recordTaskClaimed(task.getDefinitionKey(), waitingTimer);
                taskProcessingTimers.put(taskId, processingTimer);
                
                span.setAttribute("task.definition.key", task.getDefinitionKey())
                    .setAttribute("process.instance.id", task.getProcessInstanceId());
            }
            
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @Around("execution(* com.abada.engine.core.TaskManager.completeTask(..))")
    public Object traceTaskCompletion(ProceedingJoinPoint joinPoint) throws Throwable {
        String taskId = (String) joinPoint.getArgs()[0];
        
        Span span = tracer.spanBuilder("abada.task.complete")
            .setAttribute("task.id", taskId)
            .startSpan();
        
        try (var scope = span.makeCurrent()) {
            Object result = joinPoint.proceed();
            
            if (result instanceof TaskInstance task) {
                Timer.Sample processingTimer = taskProcessingTimers.remove(taskId);
                metrics.recordTaskCompleted(task.getDefinitionKey(), processingTimer);
                
                span.setAttribute("task.definition.key", task.getDefinitionKey())
                    .setAttribute("process.instance.id", task.getProcessInstanceId());
            }
            
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            if (result instanceof TaskInstance task) {
                metrics.recordTaskFailed(task.getDefinitionKey());
            }
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @Around("execution(* com.abada.engine.core.EventManager.publishEvent(..))")
    public Object traceEventPublication(ProceedingJoinPoint joinPoint) throws Throwable {
        String eventType = (String) joinPoint.getArgs()[0];
        String eventName = (String) joinPoint.getArgs()[1];
        
        Span span = tracer.spanBuilder("abada.event.publish")
            .setAttribute("event.type", eventType)
            .setAttribute("event.name", eventName)
            .startSpan();
        
        try (var scope = span.makeCurrent()) {
            metrics.recordEventPublished(eventType, eventName);
            Object result = joinPoint.proceed();
            
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    @Around("execution(* com.abada.engine.core.EventManager.correlateEvent(..))")
    public Object traceEventCorrelation(ProceedingJoinPoint joinPoint) throws Throwable {
        String eventType = (String) joinPoint.getArgs()[0];
        String eventName = (String) joinPoint.getArgs()[1];
        
        Span span = tracer.spanBuilder("abada.event.correlate")
            .setAttribute("event.type", eventType)
            .setAttribute("event.name", eventName)
            .startSpan();
        
        try (var scope = span.makeCurrent()) {
            metrics.recordEventCorrelated(eventType, eventName);
            Object result = joinPoint.proceed();
            
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Exception e) {
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}