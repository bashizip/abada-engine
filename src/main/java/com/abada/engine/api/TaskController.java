package com.abada.engine.api;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.dto.TaskDetailsDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/tasks")
public class TaskController {

    private final AbadaEngine engine;
    private final UserContextProvider context;

    public TaskController(AbadaEngine engine, UserContextProvider context) {
        this.engine = engine;
        this.context = context;
    }

    @GetMapping
    public ResponseEntity<List<TaskDetailsDto>> getTasks() {
        String user = context.getUsername();
        List<String> groups = context.getGroups();
        List<TaskInstance> visible = engine.getTaskManager().getVisibleTasksForUser(user, groups);

        List<TaskDetailsDto> taskDetailsDtos = visible.stream().map(task -> {
            ProcessInstance processInstance = engine.getProcessInstanceById(task.getProcessInstanceId());
            Map<String, Object> variables = (processInstance != null) ? processInstance.getVariables() : Map.of();
            return TaskDetailsDto.from(task, variables);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(taskDetailsDtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskDetailsDto> getTaskById(@PathVariable String id) {
        Optional<TaskInstance> taskOptional = engine.getTaskManager().getTask(id);

        if (taskOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TaskInstance task = taskOptional.get();
        ProcessInstance processInstance = engine.getProcessInstanceById(task.getProcessInstanceId());
        Map<String, Object> variables = (processInstance != null) ? processInstance.getVariables() : Map.of();

        TaskDetailsDto taskDetails = TaskDetailsDto.from(task, variables);
        return ResponseEntity.ok(taskDetails);
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
