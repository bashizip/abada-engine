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

/**
 * REST controller for managing user tasks within active process instances.
 * <p>
 * This controller provides endpoints for end-users to interact with their assigned or available tasks.
 * All operations are performed in the context of the current user, as determined by the {@link UserContextProvider}.
 * It handles listing, viewing details of, claiming, and completing tasks.
 */
@RestController
@RequestMapping("/v1/tasks")
public class TaskController {

    private final AbadaEngine engine;
    private final UserContextProvider context;

    public TaskController(AbadaEngine engine, UserContextProvider context) {
        this.engine = engine;
        this.context = context;
    }

    /**
     * Retrieves a list of tasks that are visible to the current user.
     * <p>
     * A task is considered visible if it is directly assigned to the user, or if it is unassigned
     * and the user is a member of one of the task's candidate groups.
     *
     * @return A {@link ResponseEntity} containing a list of {@link TaskDetailsDto} objects.
     *         The list will be empty if no tasks are visible to the user.
     */
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

    /**
     * Retrieves the full details of a single task by its unique ID.
     * <p>
     * This includes the task's metadata (name, assignee, etc.) as well as all current variables
     * from the process instance it belongs to.
     *
     * @param id The unique identifier of the task.
     * @return A {@link ResponseEntity} containing the {@link TaskDetailsDto} if found (200 OK),
     *         or a 404 Not Found response if no task with the given ID exists.
     */
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

    /**
     * Allows the current user to claim an unassigned task.
     * <p>
     * To successfully claim a task, the task must not have an assignee, and the current user must
     * be a candidate for the task (either by direct user candidacy or group membership).
     *
     * @param taskId The unique identifier of the task to claim.
     * @return A {@link ResponseEntity} with a confirmation message (200 OK) on success,
     *         or a 400 Bad Request response if the task cannot be claimed (e.g., already assigned or not a candidate).
     */
    @PostMapping("/claim")
    public ResponseEntity<String> claim(@RequestParam String taskId) {
        boolean claimed = engine.claim(taskId, context.getUsername(), context.getGroups());
        return claimed ? ResponseEntity.ok("Claimed") : ResponseEntity.badRequest().body("Cannot claim");
    }

    /**
     * Completes a task that is currently assigned to the user, optionally updating process variables.
     * <p>
     * The task must be assigned to the current user for the completion to be successful.
     * Any variables provided in the request body will be merged into the process instance's variable scope.
     *
     * @param taskId    The unique identifier of the task to complete.
     * @param variables An optional JSON object in the request body containing variables to be set in the process instance.
     * @return A {@link ResponseEntity} with a confirmation message (200 OK) on success,
     *         or a 400 Bad Request response if the task cannot be completed (e.g., not assigned to the user).
     */
    @PostMapping("/complete")
    public ResponseEntity<String> complete(@RequestParam String taskId, @RequestBody(required = false) Map<String, Object> variables) {
        boolean completed = engine.completeTask(taskId, context.getUsername(), context.getGroups(), variables);
        return completed ? ResponseEntity.ok("Completed") : ResponseEntity.badRequest().body("Cannot complete");
    }

}
