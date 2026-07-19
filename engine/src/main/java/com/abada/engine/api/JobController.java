package com.abada.engine.api;

import com.abada.engine.dto.FailedJobDTO;
import com.abada.engine.dto.RetriesRequest;
import com.abada.engine.core.ExternalTaskCommandService;
import com.abada.engine.core.IdempotencyService;
import com.abada.engine.persistence.entity.ExternalTaskEntity;
import com.abada.engine.persistence.repository.ExternalTaskRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

/**
 * REST controller for job/incident management in Orun Operations Cockpit.
 * Provides endpoints to list failed jobs, retry them, and view error details.
 */
@RestController
@RequestMapping("/v1/jobs")
public class JobController {

        private final ExternalTaskRepository externalTaskRepository;
        private final ExternalTaskCommandService commands;
        private final IdempotencyService idempotency;

        public JobController(ExternalTaskRepository externalTaskRepository, ExternalTaskCommandService commands,
                        IdempotencyService idempotency) {
                this.externalTaskRepository = externalTaskRepository;
                this.commands = commands;
                this.idempotency = idempotency;
        }

        /**
         * Lists failed jobs (incidents) for operations management.
         * Used by Orun to populate the "Attention Required" list.
         *
         * @param withException Filter to include only jobs with exception information
         * @param active        Filter to include only jobs that can be retried (retries
         *                      not exhausted)
         * @return List of failed jobs with incident information
         */
        @GetMapping
        public ResponseEntity<List<FailedJobDTO>> listFailedJobs(
                        @RequestParam(defaultValue = "true") boolean withException,
                        @RequestParam(defaultValue = "true") boolean active) {

                List<ExternalTaskEntity> failedTasks = externalTaskRepository.findAll().stream()
                                .filter(task -> task.getStatus() == ExternalTaskEntity.Status.FAILED ||
                                                (task.getRetries() != null && task.getRetries() <= 0))
                                .filter(task -> !withException || task.getExceptionMessage() != null)
                                .filter(task -> !active || (task.getRetries() != null && task.getRetries() >= 0))
                                .collect(Collectors.toList());

                List<FailedJobDTO> response = failedTasks.stream()
                                .map(task -> new FailedJobDTO(
                                                task.getId(),
                                                task.getProcessInstanceId(),
                                                task.getActivityId(),
                                                task.getExceptionMessage(),
                                                task.getRetries()))
                                .collect(Collectors.toList());

                return ResponseEntity.ok(response);
        }

        /**
         * Sets the retry count for a failed job, allowing it to be re-executed.
         * Used by the "Retry" button in Orun.
         *
         * @param jobId   The ID of the failed job
         * @param request The retry request containing the new retry count
         * @return Empty response on success
         */
        @PostMapping("/{jobId}/retries")
        public ResponseEntity<Void> setRetries(
                        @PathVariable String jobId,
                        @RequestBody RetriesRequest request,
                        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

                idempotency.execute(idempotencyKey, "external-task.retries",
                                Map.of("jobId", jobId, "retries", request.retries()), () -> {
                                        commands.setRetries(jobId, request.retries());
                                        return Map.of("status", "Retries updated", "jobId", jobId);
                                });
                return ResponseEntity.ok().build();
        }

        /**
         * Retrieves the full stack trace of a failed job.
         * Used by Orun when clicking "Show Error" to debug incidents.
         *
         * @param jobId The ID of the failed job
         * @return The full Java stack trace as plain text
         */
        @GetMapping(value = "/{jobId}/stacktrace", produces = MediaType.TEXT_PLAIN_VALUE)
        public ResponseEntity<String> getStacktrace(@PathVariable String jobId) {
                ExternalTaskEntity task = externalTaskRepository.findById(jobId)
                                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

                String stacktrace = task.getExceptionStacktrace();
                if (stacktrace == null || stacktrace.isEmpty()) {
                        return ResponseEntity.ok("No stack trace available for this job.");
                }

                return ResponseEntity.ok(stacktrace);
        }
}
