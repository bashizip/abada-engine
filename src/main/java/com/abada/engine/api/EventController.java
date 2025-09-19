package com.abada.engine.api;

import com.abada.engine.core.EventManager;
import com.abada.engine.dto.MessageEventRequest;
import com.abada.engine.dto.SignalEventRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/events")
public class EventController {

    private final EventManager eventManager;

    public EventController(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    /**
     * Correlates a message event to a waiting process instance.
     */
    @PostMapping("/messages")
    public ResponseEntity<Void> correlateMessage(@RequestBody MessageEventRequest request) {
        eventManager.correlateMessage(request.messageName(), request.correlationKey(), request.variables());
        return ResponseEntity.accepted().build();
    }

    /**
     * Broadcasts a signal event to all waiting process instances.
     */
    @PostMapping("/signals")
    public ResponseEntity<Void> broadcastSignal(@RequestBody SignalEventRequest request) {
        eventManager.broadcastSignal(request.signalName(), request.variables());
        return ResponseEntity.accepted().build();
    }
}
