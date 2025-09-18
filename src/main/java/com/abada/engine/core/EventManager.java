package com.abada.engine.core;

import com.abada.engine.core.model.EventMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventManager {

    private static final Logger log = LoggerFactory.getLogger(EventManager.class);

    private AbadaEngine abadaEngine;

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

    public void correlateMessage(String messageName, String correlationKey, Map<String, Object> variables) {
        Map<String, WaitingInstance> subs = messageSubscriptions.get(messageName);
        if (subs != null) {
            WaitingInstance waitingInstance = subs.remove(correlationKey);
            if (waitingInstance != null) {
                log.info("Correlated message '{}' with key '{}' to instance {}. Resuming...", messageName, correlationKey, waitingInstance.processInstanceId);
                abadaEngine.resumeFromEvent(waitingInstance.processInstanceId, waitingInstance.eventId, variables);
            }
        }
    }
	
    public void broadcastSignal(String signalName, Map<String, Object> variables) {
        List<WaitingInstance> subs = signalSubscriptions.remove(signalName);
        if (subs != null && !subs.isEmpty()) {
            log.info("Broadcasting signal '{}' to {} waiting instances.", signalName, subs.size());
            for (WaitingInstance waitingInstance : subs) {
                abadaEngine.resumeFromEvent(waitingInstance.processInstanceId, waitingInstance.eventId, variables);
            }
        } else {
            log.warn("Received signal '{}' but no instances were waiting.", signalName);
        }
    }

    private record WaitingInstance(String processInstanceId, String eventId) {}
}
