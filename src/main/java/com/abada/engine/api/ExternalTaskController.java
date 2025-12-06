package com.abada.engine.api;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.dto.ExternalTaskFailureDto;
import com.abada.engine.dto.FetchAndLockRequest;
import com.abada.engine.dto.LockedExternalTask;
import com.abada.engine.persistence.entity.ExternalTaskEntity;
import com.abada.engine.persistence.repository.ExternalTaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for external task workers.
 * This API provides the necessary endpoints for external workers to fetch,
 * lock, and complete jobs,
 * enabling the "External Task Worker" pattern.
 */
@RestController
@RequestMapping("/v1/external-tasks")
public class ExternalTaskController {

    private final ExternalTaskRepository externalTaskRepository;
    private final AbadaEngine abadaEngine;

    public ExternalTaskController(ExternalTaskRepository externalTaskRepository, AbadaEngine abadaEngine) {
        this.externalTaskRepository = externalTaskRepository;
        this.abadaEngine = abadaEngine;
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
    @Transactional
    public ResponseEntity<List<LockedExternalTask>> fetchAndLock(@RequestBody FetchAndLockRequest request) {
        List<LockedExternalTask> lockedTasks = new ArrayList<>();

        for (String topic : request.topics()) {
            Optional<ExternalTaskEntity> taskOptional = externalTaskRepository
                    .findFirstByTopicNameAndStatusOrTopicNameAndLockExpirationTimeLessThan(topic,
                            ExternalTaskEntity.Status.OPEN, topic, Instant.now());

            if (taskOptional.isPresent()) {
                ExternalTaskEntity task = taskOptional.get();
                task.setWorkerId(request.workerId());
                task.setStatus(ExternalTaskEntity.Status.LOCKED);
                task.setLockExpirationTime(Instant.now().plusMillis(request.lockDuration()));
                externalTaskRepository.save(task);

                Map<String, Object> variables = abadaEngine.getProcessInstanceById(task.getProcessInstanceId())
                        .getVariables();
                lockedTasks.add(new LockedExternalTask(task.getId(), task.getTopicName(), variables));

                return ResponseEntity.ok(lockedTasks);
            }
        }

        return ResponseEntity.ok(lockedTasks);
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
        ExternalTaskEntity task = externalTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("External task not found: " + id));

        // The service task ID in the process is the token the instance is waiting on
        String serviceTaskId = abadaEngine.getProcessInstanceById(task.getProcessInstanceId()).getActiveTokens().get(0);

        abadaEngine.resumeFromEvent(task.getProcessInstanceId(), serviceTaskId, variables);
        externalTaskRepository.delete(task);

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
        ExternalTaskEntity task = externalTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("External task not found: " + id));

        if (task.getWorkerId() != null && !task.getWorkerId().equals(failureDto.workerId())) {
            // In a real scenario, we might want to reject this, but for now we'll allow it
            // or just log a warning
            // throw new RuntimeException("Worker ID mismatch");
        }

        task.setExceptionMessage(failureDto.errorMessage());
        task.setExceptionStacktrace(failureDto.errorDetails());
        task.setRetries(failureDto.retries());

        if (failureDto.retries() != null && failureDto.retries() == 0) {
            task.setStatus(ExternalTaskEntity.Status.FAILED);
            task.setLockExpirationTime(null);
            task.setWorkerId(null);
        } else {
            task.setStatus(ExternalTaskEntity.Status.OPEN);
            task.setLockExpirationTime(null); // Release lock so it can be picked up again immediately or after timeout
            task.setWorkerId(null);
            // TODO: Implement retry timeout (backoff)
        }

        externalTaskRepository.save(task);

        return ResponseEntity.ok().build();
    }
}
