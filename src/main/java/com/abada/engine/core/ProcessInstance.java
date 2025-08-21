package com.abada.engine.core;

import com.abada.engine.core.model.GatewayMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.core.model.SequenceFlow;
import com.abada.engine.core.model.TaskMeta;
import com.abada.engine.dto.UserTaskPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProcessInstance {

    private final String id;
    private final ParsedProcessDefinition definition;
    private String currentActivityId;
    private final Map<String, Object> variables = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(ProcessInstance.class);

    public ProcessInstance(ParsedProcessDefinition definition) {
        this.id = UUID.randomUUID().toString();
        this.definition = definition;
        this.currentActivityId = definition.getStartEventId();
    }

    public ProcessInstance(String id, ParsedProcessDefinition definition, String currentActivityId) {
        this.id = id;
        this.definition = definition;
        this.currentActivityId = currentActivityId;
    }

    public String getId() { return id; }
    public ParsedProcessDefinition getDefinition() { return definition; }
    public String getCurrentActivityId() { return currentActivityId; }
    public void setCurrentActivityId(String currentActivityId) { this.currentActivityId = currentActivityId; }

    public void setVariable(String key, Object value) { variables.put(key, value); }
    public Object getVariable(String key) { return variables.get(key); }
    public Map<String, Object> getVariables() { return Collections.unmodifiableMap(variables); }

    public boolean isWaitingForUserTask() {
        return currentActivityId != null && definition.isUserTask(currentActivityId);
    }

    public boolean isCompleted() { return currentActivityId == null; }

    public Optional<UserTaskPayload> advance() {
        Map<String, Object> vars = this.variables;
        String pointer = this.currentActivityId;

        // Initialize pointer from StartEvent if not yet set
        if (pointer == null || pointer.isBlank()) {
            pointer = definition.getStartEventId();
            if (log.isDebugEnabled()) log.debug("pi={} start at {}", id, pointer);
        }

        GatewaySelector selector = new GatewaySelector();
        int hops = 0;
        final int MAX_HOPS = 2048; // guard against malformed cycles

        while (true) {
            if (++hops > MAX_HOPS) {
                throw new IllegalStateException("advance() exceeded max hops; possible cycle without wait state. pi=" + id);
            }

            // USER TASK → stop and let caller create a TaskInstance
            if (definition.isUserTask(pointer)) {
                this.currentActivityId = pointer;
                TaskMeta ut = definition.getUserTask(pointer);
                if (log.isDebugEnabled()) log.debug("pi={} reached user task {} ({})", id, ut.getId(), ut.getName());
                return Optional.of(new UserTaskPayload(
                        ut.getId(),
                        ut.getName(),
                        ut.getAssignee(),
                        ut.getCandidateUsers(),
                        ut.getCandidateGroups()
                ));
            }

            // EXCLUSIVE GATEWAY → evaluate conditions using current variables
            if (definition.isExclusiveGateway(pointer)) {
                GatewayMeta gw = definition.getGateways().get(pointer);
                List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
                String chosenFlowId = selector.chooseOutgoing(gw, outgoing, vars);

                // resolve target node id for the chosen flow
                String target = outgoing.stream()
                        .filter(f -> Objects.equals(f.getId(), chosenFlowId))
                        .map(SequenceFlow::getTargetRef)
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Flow not found: " +
                                chosenFlowId + " from gw=" + gw.id()));

                if (log.isDebugEnabled()) log.debug("pi={} gateway {} -> flow {} -> {}", id, gw.id(), chosenFlowId, target);
                pointer = target;
                continue;
            }

            // TODO: INCLUSIVE GATEWAY semantics (prepare future support)
            // if (definition.isInclusiveGateway(pointer)) {
            //     // Placeholder: handle multiple true branches (fork) and join behavior
            // }

            // END EVENT → finish
            if (definition.isEndEvent(pointer)) {
                // mark completed by nulling the pointer so isCompleted() returns true
                this.currentActivityId = null;
                if (log.isDebugEnabled()) log.debug("pi={} ended at {}", id, pointer);
                return Optional.empty();
            }

            // FALLTHROUGH NODES (start, service/script tasks, pass-through elements)
            List<SequenceFlow> outgoing = definition.getOutgoing(pointer);
            if (outgoing == null || outgoing.isEmpty()) {
                // No outgoing: treat as terminal
                this.currentActivityId = null;
                if (log.isDebugEnabled()) log.debug("pi={} no outgoing from {}; treating as end", id, pointer);
                return Optional.empty();
            }

            // Default behavior: follow the first outgoing sequence flow
            pointer = outgoing.get(0).getTargetRef();
            if (log.isDebugEnabled()) log.debug("pi={} pass-through {} -> {}", id, this.currentActivityId, pointer);
            this.currentActivityId = pointer;
        }
    }
}
