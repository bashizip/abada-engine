package com.abada.engine.api;

import com.abada.engine.core.ExternalTaskCommandService;
import com.abada.engine.dto.ExternalTaskFailureDto;
import com.abada.engine.dto.FetchAndLockRequest;
import com.abada.engine.dto.LockedExternalTask;
import com.abada.engine.dto.ExtendLockRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for external task workers.
 * This API provides the necessary endpoints for external workers to fetch,
 * lock, and complete jobs,
 * enabling the "External Task Worker" pattern.
 */
@RestController
@RequestMapping("/v1/external-tasks")
public class ExternalTaskController {

    private final ExternalTaskCommandService commands;

    public ExternalTaskController(ExternalTaskCommandService commands) {
        this.commands = commands;
    }

    /**
     * Fetches and locks available external tasks for a given set of topics.
     * This endpoint is designed to be polled by external task workers.
     * The method is transactional and will attempt to find and lock one available
     * task.
     *
     * @param request The request body containing the worker ID, a list of topics
     *                the worker can handle,
     *                and the desired lock duration in milliseconds.
     * @return A ResponseEntity containing a list of locked tasks. The list will
     *         contain at most one task,
     *         or be empty if no tasks are available for the given topics.
     */
    @PostMapping("/fetch-and-lock")
    public ResponseEntity<List<LockedExternalTask>> fetchAndLock(@RequestBody FetchAndLockRequest request) {
        return ResponseEntity.ok(commands.fetchAndLock(request));
    }

    /**
     * Completes an external task and resumes the corresponding process instance.
     * This endpoint is called by a worker after it has successfully finished its
     * business logic.
     *
     * @param id        The unique ID of the external task to complete. This is the
     *                  ID that was returned
     *                  in the `LockedExternalTask` payload from the
     *                  `fetch-and-lock` endpoint.
     *                  It is **not** the topic name.
     * @param variables A map of variables to pass back to the process instance.
     *                  These variables will be
     *                  merged into the process scope before the engine advances.
     * @return An HTTP 200 OK response on successful completion.
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<Void> complete(@PathVariable String id, @RequestBody Map<String, Object> variables) {
        commands.complete(id, variables);
        return ResponseEntity.ok().build();
    }

    /**
     * Reports a failure for an external task.
     * This endpoint is called by a worker when it fails to process a task.
     *
     * @param id         The unique ID of the external task.
     * @param failureDto The failure details.
     * @return An HTTP 200 OK response.
     */
    @PostMapping("/{id}/failure")
    public ResponseEntity<Void> handleFailure(@PathVariable String id, @RequestBody ExternalTaskFailureDto failureDto) {
        commands.handleFailure(id, failureDto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/extend-lock")
    public ResponseEntity<Void> extendLock(@PathVariable String id, @RequestBody ExtendLockRequest request) {
        commands.extendLock(id, request);
        return ResponseEntity.noContent().build();
    }
}
