package com.abada.engine.api;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.TaskInstance;
import com.abada.engine.persistence.entity.ProcessDefinitionEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/abada/process")
public class AbadaEngineController {

    private final AbadaEngine engine;
    private final UserContextProvider context;

    public AbadaEngineController(AbadaEngine engine, UserContextProvider context) {
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
        String instanceId = engine.startProcess(processId);

        return ResponseEntity.ok("Started instance: " + instanceId);
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskInstance>> tasks() {
        String user = context.getUsername();
        List<String> groups = context.getGroups();
        List<TaskInstance> visible = engine.getVisibleTasks(user, groups);
        return ResponseEntity.ok(visible);
    }



    @PostMapping("/claim")
    public ResponseEntity<String> claim(@RequestParam String taskId) {
        boolean claimed = engine.claim(taskId, context.getUsername(), context.getGroups());
        return claimed ? ResponseEntity.ok("Claimed") : ResponseEntity.badRequest().body("Cannot claim");
    }

    @PostMapping("/complete")
    public ResponseEntity<String> complete(@RequestParam String taskId) {
        boolean completed = engine.complete(taskId, context.getUsername(), context.getGroups());
        return completed ? ResponseEntity.ok("Completed") : ResponseEntity.badRequest().body("Cannot complete");
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
