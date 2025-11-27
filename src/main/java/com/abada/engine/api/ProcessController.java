package com.abada.engine.api;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.dto.Mapper;
import com.abada.engine.dto.ProcessInstanceDTO;
import com.abada.engine.dto.VariablePatchRequest;
import com.abada.engine.dto.VariableValue;
import com.abada.engine.dto.CancelRequest;
import com.abada.engine.dto.SuspensionRequest;
import com.abada.engine.dto.ActivityInstanceTree;
import com.abada.engine.dto.ChildActivityInstance;
import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for managing BPMN process definitions and instances.
 * Provides endpoints to deploy, list, and start processes, as well as query
 * their status and manage process variables.
 */
@RestController
@RequestMapping("/v1/processes")
public class ProcessController {

    private final AbadaEngine engine;
    private final UserContextProvider context;

    public ProcessController(AbadaEngine engine, UserContextProvider context) {
        this.engine = engine;
        this.context = context;
    }

    /**
     * Deploys a new BPMN process definition from an XML file.
     *
     * @param file The BPMN 2.0 XML file as a multipart request part.
     * @return A JSON object confirming successful deployment.
     * @throws IOException If the file cannot be read.
     */
    @PostMapping(value = "/deploy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> deploy(@RequestParam("file") MultipartFile file) throws IOException {
        engine.deploy(file.getInputStream());
        return ResponseEntity.ok(Map.of("status", "Deployed"));
    }

    /**
     * Lists all currently deployed process definitions.
     * Each item includes 'id', 'name', 'documentation', and 'bpmnXml'.
     *
     * @return A list of all process definitions.
     */
    @GetMapping
    public List<ProcessDefinitionEntity> listProcesses() {
        return engine.getDeployedProcesses();
    }

    /**
     * Starts a new instance of a specified process definition.
     * Optionally accepts a username parameter to track who initiated the process.
     *
     * @param processId    The ID of the process definition to start.
     * @param username     Optional username who starts the process (defaults to
     *                     "system")
     * @param variablesMap Optional initial process variables
     * @return A JSON object with the new process instance ID.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startProcess(
            @RequestParam String processId,
            @RequestParam(required = false) String username,
            @RequestBody(required = false) Map<String, Object> variablesMap) {

        ProcessInstance instance = engine.startProcess(processId, username);

        if (variablesMap != null && !variablesMap.isEmpty()) {
            instance.putAllVariables(variablesMap);
        }

        return ResponseEntity.ok(Map.of("processInstanceId", instance.getId()));
    }

    /**
     * Lists all process instances across all process definitions.
     * Returns full ProcessInstanceDTO records including status and metadata.
     *
     * @return A list of all active and completed process instances.
     */
    @GetMapping("/instances")
    public ResponseEntity<List<ProcessInstanceDTO>> listAllProcessInstances() {
        List<ProcessInstanceDTO> instances = engine.getAllProcessInstances().stream()
                .map(Mapper.ProcessInstanceMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(instances);
    }

    /**
     * Retrieves the details of a specific process instance by ID.
     * Returns a ProcessInstanceDTO with status, dates, and metadata.
     *
     * @param instanceId The ID of the process instance.
     * @return The process instance details, or 404 if not found.
     */
    @GetMapping("/instances/{instanceId}")
    public ResponseEntity<ProcessInstanceDTO> getProcessInstance(@PathVariable String instanceId) {
        ProcessInstance instance = engine.getProcessInstanceById(instanceId);
        if (instance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Mapper.ProcessInstanceMapper.toDto(instance));
    }

    /**
     * Gets all variables for a specific process instance.
     * Used by Orun for the "Data Surgery" feature to view current state.
     *
     * @param instanceId The ID of the process instance.
     * @return A map of variable names to their typed values.
     */
    @GetMapping("/api/v1/process-instances/{instanceId}/variables")
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
    @PatchMapping("/api/v1/process-instances/{instanceId}/variables")
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
    @DeleteMapping("/api/v1/process-instances/{id}")
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
    @PutMapping("/api/v1/process-instances/{id}/suspension")
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
     * Marks a process instance as FAILED.
     * This is a terminal status that stops all execution of the instance.
     *
     * @param id The unique ID of the process instance to fail.
     * @return A JSON object confirming the status change.
     */
    @PostMapping("/instance/{id}/fail")
    public ResponseEntity<Map<String, Object>> failInstance(@PathVariable String id) {
        boolean failed = engine.failProcess(id);
        if (failed) {
            return ResponseEntity.ok(Map.of("status", "Failed", "processInstanceId", id));
        } else {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Cannot fail process instance", "processInstanceId", id));
        }
    }

    /**
     * Retrieves the active activity instances for a process instance.
     * Used by Orun for visual BPMN highlighting.
     *
     * @param id The ID of the process instance.
     * @return The activity instance tree containing active tokens.
     */
    @GetMapping("/api/v1/process-instances/{id}/activity-instances")
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

    /**
     * Retrieves a specific process definition by its ID.
     *
     * @param id The ID of the process definition (as defined in the BPMN file).
     * @return A map containing the definition's 'id', 'name', 'documentation', and
     *         the full 'bpmnXml'. Returns 404 Not Found if the ID is not found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getProcessById(@PathVariable String id) {
        return engine.getProcessDefinitionById(id)
                .map(def -> {
                    Map<String, String> responseMap = new HashMap<>();
                    responseMap.put("id", def.getId());
                    responseMap.put("name", def.getName());
                    responseMap.put("documentation", def.getDocumentation()); // Safely handles null
                    responseMap.put("bpmnXml", def.getBpmnXml());
                    return responseMap;
                })
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
