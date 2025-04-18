package com.abada.engine.api;

import com.abada.engine.core.*;
import com.abada.engine.parser.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/engine")
public class AbadaEngineController {

    private final AbadaEngine engine = new AbadaEngine();
    private final BpmnParser parser = new BpmnParser();

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
    public ResponseEntity<List<TaskInstance>> tasks(@RequestParam String user,
                                                    @RequestParam(required = false) String groups) {
        List<String> groupList = groups != null ? Arrays.asList(groups.split(",")) : Collections.emptyList();
        return ResponseEntity.ok(engine.getVisibleTasks(user, groupList));
    }

    @PostMapping("/claim")
    public ResponseEntity<String> claim(@RequestParam String taskId,
                                        @RequestParam String user,
                                        @RequestParam(required = false) String groups) {
        List<String> groupList = groups != null ? Arrays.asList(groups.split(",")) : Collections.emptyList();
        boolean claimed = engine.claimTask(taskId, user, groupList);
        return claimed ? ResponseEntity.ok("Claimed") : ResponseEntity.badRequest().body("Cannot claim");
    }
}
