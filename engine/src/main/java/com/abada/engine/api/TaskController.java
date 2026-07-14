package com.abada.engine.api;

import com.abada.engine.context.UserContextProvider;
import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.UserStatsService;
import com.abada.engine.core.IdempotencyService;
import com.abada.engine.core.model.TaskInstance;
import com.abada.engine.core.model.TaskStatus;
import com.abada.engine.dto.TaskDetailsDto;
import com.abada.engine.dto.UserStatsDto;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final UserStatsService userStatsService;
    private final IdempotencyService idempotencyService;

    public TaskController(
        AbadaEngine engine,
        UserContextProvider context,
        UserStatsService userStatsService,
        IdempotencyService idempotencyService
    ) {
        this.engine = engine;
        this.context = context;
        this.userStatsService = userStatsService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * Retrieves a bounded page of tasks visible to the current user, with optional filtering by status.
     * <p>
     * A task is considered visible if it is directly assigned to the user, or if it is unassigned
     * and the user is a member of one of the task's candidate groups.
     *
     * @param status (Optional) The status to filter tasks by (e.g., AVAILABLE, CLAIMED).
     * @return A {@link ResponseEntity} containing a list of {@link TaskDetailsDto} objects.
     */
    @GetMapping
    public ResponseEntity<List<TaskDetailsDto>> getTasks(
        @RequestParam(required = false) TaskStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = Pagination.DEFAULT_PAGE_SIZE) int size
    ) {
        String user = context.getUsername();
        List<String> groups = context.getGroups();
        Pageable pageable = Pagination.request(page, size,
                Sort.by("startDate").ascending().and(Sort.by("id").ascending()));
        Page<TaskInstance> visible = engine
            .getTaskManager()
            .getVisibleTasksForUser(user, groups, status, pageable);

        Set<String> processInstanceIds = visible.stream()
                .map(TaskInstance::getProcessInstanceId)
                .collect(Collectors.toSet());
        Map<String, ProcessInstance> processInstances = engine.getProcessInstancesByIds(processInstanceIds);

        List<TaskDetailsDto> taskDetailsDtos = visible.getContent()
            .stream()
            .map(task -> TaskDetailsDto.from(task, processInstances.get(task.getProcessInstanceId())))
            .collect(Collectors.toList());

        return ResponseEntity.ok()
                .headers(Pagination.headers(visible))
                .body(taskDetailsDtos);
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
        Optional<TaskInstance> taskOptional = engine.getTaskById(id);

        if (taskOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TaskInstance task = taskOptional.get();
        ProcessInstance processInstance = engine.getProcessInstanceById(
            task.getProcessInstanceId()
        );

        TaskDetailsDto taskDetails = TaskDetailsDto.from(task, processInstance);
        return ResponseEntity.ok(taskDetails);
    }

    /**
     * Allows the current user to claim an unassigned task.
     * Any exceptions (e.g., task not available) are handled by the GlobalExceptionHandler.
     *
     * @param taskId The unique identifier of the task to claim.
     * @return A {@link ResponseEntity} with a JSON object confirming success (200 OK).
     */
    @PostMapping("/claim")
    public ResponseEntity<Map<String, Object>> claim(
        @RequestParam String taskId
    ) {
        engine.claim(taskId, context.getUsername(), context.getGroups());
        return ResponseEntity.ok(Map.of("status", "Claimed", "taskId", taskId));
    }

    /**
     * Completes a task that is currently assigned to the user, optionally updating process variables.
     * Any exceptions (e.g., user not authorized) are handled by the GlobalExceptionHandler.
     *
     * @param taskId    The unique identifier of the task to complete.
     * @param variables An optional JSON object in the request body containing variables to be set in the process instance.
     * @return A {@link ResponseEntity} with a JSON object confirming success (200 OK).
     */
    @PostMapping("/complete")
    public ResponseEntity<Map<String, Object>> complete(
        @RequestParam String taskId,
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @RequestBody(required = false) Map<String, Object> variables
    ) {
        Map<String, Object> body = variables == null ? Map.of() : variables;
        Map<String, Object> response = idempotencyService.execute(idempotencyKey, "task.complete",
                Map.of("taskId", taskId, "variables", body), () -> {
                    engine.completeTask(taskId, context.getUsername(), context.getGroups(), body);
                    return Map.of("status", "Completed", "taskId", taskId);
                });
        return ResponseEntity.ok(response);
    }

    /**
     * Marks a task as FAILED.
     * Any exceptions (e.g., task not found) are handled by the GlobalExceptionHandler.
     *
     * @param taskId The unique identifier of the task to fail.
     * @return A {@link ResponseEntity} with a JSON object confirming success (200 OK).
     */
    @PostMapping("/fail")
    public ResponseEntity<Map<String, Object>> fail(
        @RequestParam String taskId
    ) {
        engine.failTask(taskId);
        return ResponseEntity.ok(Map.of("status", "Failed", "taskId", taskId));
    }

    /**
     * Retrieves comprehensive statistics and activity data for the current user.
     * <p>
     * This endpoint provides:
     * - Quick stats (active tasks, completed tasks, running processes, available tasks)
     * - Recent tasks assigned to the user (last 10)
     * - Tasks grouped by status
     * - Overdue tasks (CLAIMED for more than 7 days)
     * - Process activity (recently started processes, active process count, completion rate)
     *
     * @return A {@link ResponseEntity} containing a {@link UserStatsDto} with all user statistics.
     */
    @GetMapping("/user-stats")
    public ResponseEntity<UserStatsDto> getUserStats() {
        String username = context.getUsername();
        List<String> userGroups = context.getGroups();

        UserStatsDto stats = userStatsService.getUserStats(
            username,
            userGroups
        );
        return ResponseEntity.ok(stats);
    }
}
