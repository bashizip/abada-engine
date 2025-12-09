package com.abada.engine.api;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.dto.VariablePatchRequest;
import com.abada.engine.dto.VariableValue;
import com.abada.engine.dto.CancelRequest;
import com.abada.engine.dto.SuspensionRequest;
import com.abada.engine.dto.ActivityInstanceTree;
import com.abada.engine.dto.ChildActivityInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for Operations Cockpit (Orun) endpoints.
 * Provides endpoints for managing process instances, variables, and activity
 * visualization.
 */
@RestController
@RequestMapping("/v1/process-instances")
public class CockpitProcessInstanceController {

    private final AbadaEngine engine;

    public CockpitProcessInstanceController(AbadaEngine engine) {
        this.engine = engine;
    }

    /**
     * Gets all variables for a specific process instance.
     * Used by Orun for the "Data Surgery" feature to view current state.
     *
     * @param instanceId The ID of the process instance.
     * @return A map of variable names to their typed values.
     */
    @GetMapping("/{instanceId}/variables")
    public ResponseEntity<Map<String, VariableValue>> getProcessVariables(@PathVariable String instanceId) {
        ProcessInstance instance = engine.getProcessInstanceById(instanceId);
        if (instance == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, VariableValue> typedVariables = instance.getVariables().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> VariableValue.from(entry.getValue())));

        return ResponseEntity.ok(typedVariables);
    }

    /**
     * Modifies variables for a specific process instance.
     * Used by Orun for the "Data Surgery" feature to fix incorrect state.
     *
     * @param instanceId The ID of the process instance.
     * @param request    The variable modifications to apply.
     * @return Empty response on success.
     */
    @PatchMapping("/{instanceId}/variables")
    public ResponseEntity<Void> patchProcessVariables(
            @PathVariable String instanceId,
            @RequestBody VariablePatchRequest request) {

        ProcessInstance instance = engine.getProcessInstanceById(instanceId);
        if (instance == null) {
            return ResponseEntity.notFound().build();
        }

        // Apply the variable modifications
        Map<String, Object> modifications = request.modifications().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().toObject()));

        instance.putAllVariables(modifications);

        return ResponseEntity.ok().build();
    }

    /**
     * Cancels a running process instance.
     * The instance status will be set to CANCELLED and execution will stop.
     *
     * @param id      The ID of the process instance.
     * @param request Optional request body containing the cancellation reason.
     * @return Empty response on success.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelProcess(@PathVariable String id,
            @RequestBody(required = false) CancelRequest request) {
        String reason = request != null ? request.reason() : "Cancelled via API";
        try {
            engine.cancelProcessInstance(id, reason);
            return ResponseEntity.noContent().build();
        } catch (com.abada.engine.core.exception.ProcessEngineException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Suspends or activates a process instance.
     * A suspended process cannot advance or complete tasks.
     *
     * @param id      The ID of the process instance.
     * @param request The suspension request containing the new state.
     * @return Empty response on success.
     */
    @PutMapping("/{id}/suspension")
    public ResponseEntity<Void> setSuspension(@PathVariable String id,
            @RequestBody SuspensionRequest request) {
        try {
            engine.suspendProcessInstance(id, request.suspended());
            return ResponseEntity.ok().build();
        } catch (com.abada.engine.core.exception.ProcessEngineException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retrieves the active activity instances for a process instance.
     * Used by Orun for visual BPMN highlighting.
     *
     * @param id The ID of the process instance.
     * @return The activity instance tree containing active tokens.
     */
    @GetMapping("/{id}/activity-instances")
    public ResponseEntity<ActivityInstanceTree> getActivityInstances(@PathVariable String id) {
        ProcessInstance instance = engine.getProcessInstanceById(id);
        if (instance == null) {
            return ResponseEntity.notFound().build();
        }

        List<ChildActivityInstance> children = instance.getActiveTokens().stream()
                .map(activityId -> {
                    String name = instance.getDefinition().getActivityName(activityId);
                    return new ChildActivityInstance(
                            activityId,
                            name,
                            "exec-" + id // Simplified execution ID
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ActivityInstanceTree(id, children));
    }
}
