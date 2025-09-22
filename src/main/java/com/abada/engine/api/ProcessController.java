package com.abada.engine.api;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.dto.Mapper;
import com.abada.engine.dto.ProcessInstanceDTO;
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
 * Provides endpoints to deploy, list, and start processes, as well as query their status.
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
     *
     * @return A list of maps, where each map contains the 'id', 'name', and 'documentation' of a deployed process.
     */
    @GetMapping
    public ResponseEntity<List<Map<String, String>>> listDeployedProcesses() {
        List<ProcessDefinitionEntity> definitions = engine.getDeployedProcesses();

        List<Map<String, String>> result = definitions.stream()
                .map(def -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", def.getId());
                    map.put("name", def.getName());
                    map.put("documentation", def.getDocumentation()); // Safely handles null
                    return map;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Starts a new instance of a previously deployed process definition.
     *
     * @param processId The ID of the process definition to start (as defined in the BPMN file).
     * @return A JSON object containing the unique ID of the new process instance.
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> start(@RequestParam("processId") String processId) {
        ProcessInstance instance = engine.startProcess(processId);
        Map<String, String> response = Map.of("processInstanceId", instance.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a list of all process instances, both active and completed.
     *
     * @return A list of {@link ProcessInstanceDTO} objects representing the process instances.
     */
    @GetMapping("/instances")
    public ResponseEntity<List<ProcessInstanceDTO>> getAllProcessInstances() {
        List<ProcessInstanceDTO> instances = engine.getAllProcessInstances().stream()
                .map(Mapper.ProcessInstanceMapper::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(instances);
    }

    /**
     * Retrieves a single process instance by its unique ID.
     *
     * @param id The UUID of the process instance.
     * @return A {@link ProcessInstanceDTO} with the instance details, or a 404 Not Found if no instance matches the ID.
     */
    @GetMapping("/instance/{id}")
    public ResponseEntity<ProcessInstanceDTO> getProcessInstanceById(@PathVariable String id) {
        ProcessInstance pi = engine.getProcessInstanceById(id);
        if (pi == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Mapper.ProcessInstanceMapper.toDto(pi));
    }

    /**
     * Retrieves the details of a specific process definition by its ID.
     *
     * @param id The ID of the process definition (as defined in the BPMN file).
     * @return A map containing the definition's 'id', 'name', 'documentation', and the full 'bpmnXml'. Returns 404 Not Found if the ID is not found.
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
