package com.abada.engine.core.model;

import java.io.Serializable;

/**
 * Represents the metadata of a parsed intermediate catch event from a BPMN file.
 *
 * @param id The ID of the event node.
 * @param name The name of the event.
 * @param type The type of the event (e.g., MESSAGE, SIGNAL, TIMER).
 * @param definitionRef The specific reference for the event, such as a message name or signal name.
 */
public record EventMeta(String id,
                        String name,
                        EventType type,
                        String definitionRef) implements Serializable {

    public enum EventType {
        MESSAGE,
        TIMER,
        SIGNAL,
        CONDITIONAL
    }
}
