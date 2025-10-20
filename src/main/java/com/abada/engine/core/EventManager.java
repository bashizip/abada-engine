package com.abada.engine.core;

import com.abada.engine.core.model.EventMeta;
import com.abada.engine.observability.EngineMetrics;
import io.micrometer.tracing.annotation.SpanTag;
import io.micrometer.tracing.annotation.WithSpan;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventManager {

    private static final Logger log = LoggerFactory.getLogger(EventManager.class);

    private AbadaEngine abadaEngine;
    private final EngineMetrics engineMetrics;
    private final Tracer tracer;

    @Autowired
    public EventManager(EngineMetrics engineMetrics, Tracer tracer) {
        this.engineMetrics = engineMetrics;
        this.tracer = tracer;
    }

    // Registry for message-based event subscriptions: <MessageName, <CorrelationKey, WaitingInstance>>
    private final Map<String, Map<String, WaitingInstance>> messageSubscriptions = new ConcurrentHashMap<>();

    // Registry for signal-based event subscriptions: <SignalName, List<WaitingInstance>>
    private final Map<String, List<WaitingInstance>> signalSubscriptions = new ConcurrentHashMap<>();

    public void setAbadaEngine(AbadaEngine abadaEngine) {
        this.abadaEngine = abadaEngine;
    }

    /**
     * Registers a process instance that is waiting for one or more events.
     */
    public void registerWaitStates(ProcessInstance instance) {
        for (String tokenId : instance.getActiveTokens()) {
            if (instance.getDefinition().isCatchEvent(tokenId)) {
                EventMeta eventMeta = instance.getDefinition().getEvents().get(tokenId);
                if (eventMeta == null) continue;

                switch (eventMeta.type()) {
                    case MESSAGE -> registerMessageSubscription(instance, eventMeta);
                    case SIGNAL -> registerSignalSubscription(instance, eventMeta);
                    case CONDITIONAL -> log.debug("Conditional events not yet implemented for instance {}", instance.getId());
                    // Timer events are handled by the JobScheduler, not here.
                }
            }
        }
    }

    private void registerMessageSubscription(ProcessInstance instance, EventMeta eventMeta) {
        String correlationKey = (String) instance.getVariable("correlationKey");
        if (correlationKey == null) {
            log.warn("Instance {} is waiting for message '{}' but has no correlationKey variable.", instance.getId(), eventMeta.definitionRef());
            return;
        }
        String messageName = eventMeta.definitionRef();
        messageSubscriptions.computeIfAbsent(messageName, k -> new ConcurrentHashMap<>()).put(correlationKey, new WaitingInstance(instance.getId(), eventMeta.id()));
        log.info("Registered instance {} waiting for message '{}' with key '{}'", instance.getId(), messageName, correlationKey);
    }

    private void registerSignalSubscription(ProcessInstance instance, EventMeta eventMeta) {
        String signalName = eventMeta.definitionRef();
        signalSubscriptions.computeIfAbsent(signalName, k -> new ArrayList<>()).add(new WaitingInstance(instance.getId(), eventMeta.id()));
        log.info("Registered instance {} waiting for signal '{}'", instance.getId(), signalName);
    }

    @WithSpan("abada.event.correlate.message")
    public void correlateMessage(@SpanTag("event.name") String messageName, 
                                @SpanTag("correlation.key") String correlationKey, 
                                Map<String, Object> variables) {
        Span span = tracer.spanBuilder("abada.event.correlate.message").startSpan();
        
        try (var scope = span.makeCurrent()) {
            span.setAttribute("event.name", messageName);
            span.setAttribute("event.type", "MESSAGE");
            span.setAttribute("correlation.key", correlationKey);
            
            Map<String, WaitingInstance> subs = messageSubscriptions.get(messageName);
            if (subs != null) {
                WaitingInstance waitingInstance = subs.remove(correlationKey);
                if (waitingInstance != null) {
                    span.setAttribute("process.instance.id", waitingInstance.processInstanceId);
                    span.setAttribute("event.id", waitingInstance.eventId);
                    
                    log.info("Correlated message '{}' with key '{}' to instance {}. Resuming...", messageName, correlationKey, waitingInstance.processInstanceId);
                    abadaEngine.resumeFromEvent(waitingInstance.processInstanceId, waitingInstance.eventId, variables);
                    
                    engineMetrics.recordEventCorrelated("MESSAGE", messageName);
                } else {
                    span.setAttribute("correlation.result", "no_matching_instance");
                }
            } else {
                span.setAttribute("correlation.result", "no_subscriptions");
            }
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
	
    @WithSpan("abada.event.broadcast.signal")
    public void broadcastSignal(@SpanTag("event.name") String signalName, Map<String, Object> variables) {
        Span span = tracer.spanBuilder("abada.event.broadcast.signal").startSpan();
        
        try (var scope = span.makeCurrent()) {
            span.setAttribute("event.name", signalName);
            span.setAttribute("event.type", "SIGNAL");
            
            List<WaitingInstance> subs = signalSubscriptions.remove(signalName);
            if (subs != null && !subs.isEmpty()) {
                span.setAttribute("instances.count", subs.size());
                log.info("Broadcasting signal '{}' to {} waiting instances.", signalName, subs.size());
                for (WaitingInstance waitingInstance : subs) {
                    abadaEngine.resumeFromEvent(waitingInstance.processInstanceId, waitingInstance.eventId, variables);
                    engineMetrics.recordEventCorrelated("SIGNAL", signalName);
                }
            } else {
                span.setAttribute("instances.count", 0);
                log.warn("Received signal '{}' but no instances were waiting.", signalName);
            }
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    private record WaitingInstance(String processInstanceId, String eventId) {}
}
