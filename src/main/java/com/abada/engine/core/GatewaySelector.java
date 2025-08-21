package com.abada.engine.core;

import com.abada.engine.core.model.GatewayMeta;
import com.abada.engine.core.model.SequenceFlow;
import com.abada.engine.util.ConditionEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Picks the outgoing SequenceFlow for an Exclusive Gateway based on conditions
 * and the gateway's default flow. Designed to be called from the engine's
 * advance loop.
 */
public final class GatewaySelector {
    private static final Logger log = LoggerFactory.getLogger(GatewaySelector.class);

    /**
     * Decide which outgoing flow to take.
     *
     * @param gw        Gateway metadata (must provide getId() and getDefaultFlowId()).
     * @param outgoing  All outgoing sequences flow for this gateway, in model order.
     * @param vars      Process instance variables available to condition evaluation.
     * @return          The id of the choetDefaultFlowIdsen SequenceFlow.
     */
    public String chooseOutgoing(GatewayMeta gw, List<SequenceFlow> outgoing, Map<String, Object> vars) {
        Objects.requireNonNull(gw, "gw");
        Objects.requireNonNull(outgoing, "outgoing");

        String defaultFlowId = gw.defaultFlowId();
        String chosen = null;

        if (log.isDebugEnabled()) {
            log.debug("Evaluating gateway id={} with {} outgoing flows; vars={}", gw.id(), outgoing.size(), vars);
        }

        for (SequenceFlow f : outgoing) {
            String cond = f.getConditionExpression(); // may be null
            boolean ok = ConditionEvaluator.evaluate(cond, vars);
            if (log.isDebugEnabled()) {
                log.debug("  flow id={} cond='{}' -> {}", f.getId(), cond, ok);
            }
            if (ok) {
                chosen = f.getId();
                break; // take the first matching condition
            }
        }

        if (chosen == null) {
            if (defaultFlowId != null) {
                if (log.isDebugEnabled()) {
                    log.debug("  no condition matched; taking default flow {}", defaultFlowId);
                }
                return defaultFlowId;
            }
            throw new IllegalStateException("No matching condition and no default flow for gateway " + gw.id());
        }
        return chosen;
    }
}

/*
 * Example usage from the engine advance loop (pseudocode):
 *
 *   GatewaySelector selector = new GatewaySelector();
 *   GatewayMeta gw = def.getGateways().get(currentGatewayId);
 *   List<SequenceFlow> outgoing = def.getOutgoing(currentGatewayId); // or equivalent API
 *   String nextFlowId = selector.chooseOutgoing(gw, outgoing, instance.getVariables());
 *   follow(nextFlowId);
 */
