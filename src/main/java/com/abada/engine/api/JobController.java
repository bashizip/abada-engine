package com.abada.engine.api;

import com.abada.engine.dto.FailedJobDTO;
import com.abada.engine.dto.RetriesRequest;
import com.abada.engine.persistence.entity.ExternalTaskEntity;
import com.abada.engine.persistence.repository.ExternalTaskRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for job/incident management in Orun Operations Cockpit.
 * Provides endpoints to list failed jobs, retry them, and view error details.
 */
@RestController
@RequestMapping("/v1/jobs")
public class JobController {

        private final ExternalTaskRepository externalTaskRepository;

        public JobController(ExternalTaskRepository externalTaskRepository) {
                this.externalTaskRepository = externalTaskRepository;
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
                        @RequestBody RetriesRequest request) {

                ExternalTaskEntity task = externalTaskRepository.findById(jobId)
                                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

                task.setRetries(request.retries());
                task.setStatus(ExternalTaskEntity.Status.OPEN); // Reset to OPEN so it can be picked up again
                task.setWorkerId(null); // Clear worker lock
                task.setLockExpirationTime(null); // Clear lock expiration

                externalTaskRepository.save(task);

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

