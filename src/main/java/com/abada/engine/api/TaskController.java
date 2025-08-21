package com.abada.engine.api;


import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.TaskInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("v1/tasks")
public class TaskController {

    private final AbadaEngine engine;
    private final UserContextProvider context; //the authenticated user that will be injected

    public TaskController(AbadaEngine engine, UserContextProvider context) {
        this.engine = engine;
        this.context = context;
    }

    // return all visible tasks for a user
    @GetMapping
    public ResponseEntity<List<TaskInstance>> tasks() {
        String user = context.getUsername();
        List<String> groups = context.getGroups();
        List<TaskInstance> visible = engine.getVisibleTasks(user, groups);
        ResponseEntity<List<TaskInstance>> res= ResponseEntity.ok(visible);
        System.out.printf(res.toString());
        return res;
    }

    @PostMapping("/claim")
    public ResponseEntity<String> claim(@RequestParam String taskId) {
        boolean claimed = engine.claim(taskId, context.getUsername(), context.getGroups());
        return claimed ? ResponseEntity.ok("Claimed") : ResponseEntity.badRequest().body("Cannot claim");
    }

    @PostMapping("/complete")
    public ResponseEntity<String> complete(@RequestParam String taskId, @RequestBody(required = false) Map<String, Object> variables) {
        boolean completed = engine.completeTask(taskId, context.getUsername(), context.getGroups(), variables);
        return completed ? ResponseEntity.ok("Completed") : ResponseEntity.badRequest().body("Cannot complete");
    }

}
