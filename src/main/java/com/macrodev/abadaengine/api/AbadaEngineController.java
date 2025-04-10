package com.macrodev.abadaengine.api;


import com.macrodev.abadaengine.core.*;
import com.macrodev.abadaengine.parser.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/engine")
public class AbadaEngineController {

    private final AbadaEngine engine = new AbadaEngine();
    private final BpmnParser parser = new BpmnParser();

    @GetMapping("/about")
    public ResponseEntity<String> about() {
      return ResponseEntity.ok("Abada Engine is running");
    }

    @PostMapping("/deploy")
    public ResponseEntity<String> deploy(@RequestParam("file") MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            BpmnParser.ParsedProcess parsed = parser.parse(input);
            ProcessDefinition def = new ProcessDefinition(
                    parsed.id, parsed.name, parsed.startEventId, parsed.userTasks, parsed.sequenceFlows);
            engine.deploy(def);
            return ResponseEntity.ok("Deployed: " + def.getId());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/start")
    public ResponseEntity<String> start(@RequestParam("processId") String processId) {
        try {
            String instanceId = engine.startProcess(processId);
            return ResponseEntity.ok("Started instance: " + instanceId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/complete")
    public ResponseEntity<String> complete(@RequestParam("taskId") String taskId) {
        try {
            engine.completeTask(taskId);
            return ResponseEntity.ok("Task completed: " + taskId);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/tasks")
    public ResponseEntity<List<TaskInstance>> tasks(@RequestParam(required = false) String user) {
        if (user != null) {
            return ResponseEntity.ok(engine.getUserTasks(user));
        }
        return ResponseEntity.ok(engine.getCandidateTasks());
    }

    @PostMapping("/claim")
    public ResponseEntity<String> claim(@RequestParam String taskId, @RequestParam String user) {
        boolean claimed = engine.claimTask(taskId, user);
        return claimed ? ResponseEntity.ok("Claimed") : ResponseEntity.badRequest().body("Cannot claim");
    }
}
