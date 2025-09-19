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
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/processes")
public class ProcessController {

    private final AbadaEngine engine;
    private final UserContextProvider context;

    public ProcessController(AbadaEngine engine, UserContextProvider context) {
        this.engine = engine;
        this.context = context;
    }

    @PostMapping(value = "/deploy", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> deploy(@RequestParam("file") MultipartFile file) throws IOException {
        engine.deploy(file.getInputStream());
        return ResponseEntity.ok("Deployed");
    }

    @GetMapping
    public ResponseEntity<List<Map<String, String>>> listDeployedProcesses() {
        List<ProcessDefinitionEntity> definitions = engine.getDeployedProcesses();

        List<Map<String, String>> result = definitions.stream()
                .map(def -> Map.of(
                        "id", def.getId(),
                        "name", def.getName()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    @PostMapping("/start")
    public ResponseEntity<String> start(@RequestParam("processId") String processId) {
        ProcessInstance instanceId = engine.startProcess(processId);

        return ResponseEntity.ok("Started instance: " + instanceId.getId());
    }

    @GetMapping("/instance/{id}")
    public ResponseEntity<ProcessInstanceDTO> getProcessInstanceById(@PathVariable String id) {
        ProcessInstance pi = engine.getProcessInstanceById(id);
        if (pi == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Mapper.ProcessInstanceMapper.toDto(pi));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, String>> getProcessById(@PathVariable String id) {
        return engine.getProcessDefinitionById(id)
                .map(def -> Map.of(
                        "id", def.getId(),
                        "name", def.getName(),
                        "bpmnXml", def.getBpmnXml()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

}
