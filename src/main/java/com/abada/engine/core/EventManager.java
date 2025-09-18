package com.abada.engine.core;

import com.abada.engine.core.model.EventMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EventManager {

    private static final Logger log = LoggerFactory.getLogger(EventManager.class);

    private AbadaEngine abadaEngine;

    // A registry for message-based event subscriptions.
    // Structure: <MessageName, <CorrelationKey, WaitingInstance>>
    private final Map<String, Map<String, WaitingInstance>> messageSubscriptions = new ConcurrentHashMap<>();

    public void setAbadaEngine(AbadaEngine abadaEngine) {
        this.abadaEngine = abadaEngine;
    }

    /**
     * Registers a process instance that is waiting for a specific event.
     *
     * @param instance The process instance that is entering a wait state.
     */
    public void registerWaitStates(ProcessInstance instance) {
        String correlationKey = (String) instance.getVariable("correlationKey");
        if (correlationKey == null) {
            // For now, we assume a correlation key is always present. This could be enhanced later.
            return;
        }

        for (String tokenId : instance.getActiveTokens()) {
            if (instance.getDefinition().isCatchEvent(tokenId)) {
                EventMeta eventMeta = instance.getDefinition().getEvents().get(tokenId);
                if (eventMeta != null && eventMeta.type() == EventMeta.EventType.MESSAGE) {
                    String messageName = eventMeta.definitionRef();
                    messageSubscriptions.computeIfAbsent(messageName, k -> new ConcurrentHashMap<>())
                            .put(correlationKey, new WaitingInstance(instance.getId(), tokenId));
                    log.info("Registered instance {} waiting for message '{}' with key '{}' at event '{}'", instance.getId(), messageName, correlationKey, tokenId);
                }
            }
        }
    }

    public void correlateMessage(String messageName, String correlationKey, Map<String, Object> variables) {
        Map<String, WaitingInstance> subs = messageSubscriptions.get(messageName);
        if (subs == null) {
            log.warn("Received message '{}' but no subscriptions exist.", messageName);
            return;
        }

        WaitingInstance waitingInstance = subs.remove(correlationKey);
        if (waitingInstance != null) {
            log.info("Correlated message '{}' with key '{}' to instance {}. Resuming...", messageName, correlationKey, waitingInstance.processInstanceId);
            abadaEngine.resumeFromEvent(waitingInstance.processInstanceId, waitingInstance.eventId, variables);
        } else {
            log.warn("Received message '{}' with key '{}' but no matching instance was found.", messageName, correlationKey);
        }

        // Clean up the map if it's empty
        if (subs.isEmpty()) {
            messageSubscriptions.remove(messageName);
        }
    }

    public void broadcastSignal(String signalName, Map<String, Object> variables) {
        // TODO: Implement signal broadcasting logic
        log.warn("Signal broadcasting is not yet implemented.");
    }

    /**
     * A record to hold information about a process instance waiting for an event.
     */
    private record WaitingInstance(String processInstanceId, String eventId) {}
}
