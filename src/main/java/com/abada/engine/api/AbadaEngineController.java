package com.abada.engine.api;

import com.abada.engine.core.*;
import com.abada.engine.parser.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/engine")
public class AbadaEngineController {

    private final AbadaEngine engine = new AbadaEngine();
    private final BpmnParser parser = new BpmnParser();

    private String getAuthenticatedUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private List<String> getAuthenticatedGroups() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
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
    public ResponseEntity<List<TaskInstance>> tasks() {
        String user = getAuthenticatedUsername();
        List<String> groups = getAuthenticatedGroups();
        return ResponseEntity.ok(engine.getVisibleTasks(user, groups));
    }

    @PostMapping("/claim")
    public ResponseEntity<String> claim(@RequestParam String taskId) {
        String user = getAuthenticatedUsername();
        List<String> groups = getAuthenticatedGroups();
        boolean claimed = engine.claimTask(taskId, user, groups);
        return claimed ? ResponseEntity.ok("Claimed") : ResponseEntity.badRequest().body("Cannot claim");
    }
}
