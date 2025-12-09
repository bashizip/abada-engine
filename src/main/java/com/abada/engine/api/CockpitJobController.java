package com.abada.engine.api;

import com.abada.engine.core.AbadaEngine;
import com.abada.engine.core.ProcessInstance;
import com.abada.engine.core.model.EventMeta;
import com.abada.engine.core.model.ParsedProcessDefinition;
import com.abada.engine.dto.ActiveJobDTO;
import com.abada.engine.dto.FailedJobDTO;
import com.abada.engine.dto.RetriesRequest;
import com.abada.engine.persistence.entity.ExternalTaskEntity;
import com.abada.engine.persistence.entity.JobEntity;
import com.abada.engine.persistence.repository.ExternalTaskRepository;
import com.abada.engine.persistence.repository.JobRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for job/incident management in Orun Operations Cockpit.
 * Provides endpoints to list failed jobs, retry them, and view error details.
 */
@RestController
@RequestMapping("/v1/jobs")
public class CockpitJobController {

        private final ExternalTaskRepository externalTaskRepository;
        private final JobRepository jobRepository;
        private final AbadaEngine abadaEngine;

        public CockpitJobController(ExternalTaskRepository externalTaskRepository, JobRepository jobRepository,
                        AbadaEngine abadaEngine) {
                this.externalTaskRepository = externalTaskRepository;
                this.jobRepository = jobRepository;
                this.abadaEngine = abadaEngine;
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
        @GetMapping("/failed")
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
         * Lists all active jobs (locked external tasks, scheduled timers, waiting
         * messages/signals).
         *
         * @return List of active jobs
         */
        @GetMapping("/active")
        public ResponseEntity<List<ActiveJobDTO>> listActiveJobs() {
                List<ActiveJobDTO> activeJobs = new ArrayList<>();

                // 1. Locked External Tasks
                List<ExternalTaskEntity> lockedTasks = externalTaskRepository.findAll().stream()
                                .filter(t -> t.getStatus() == ExternalTaskEntity.Status.LOCKED)
                                .collect(Collectors.toList());

                for (ExternalTaskEntity task : lockedTasks) {
                        activeJobs.add(new ActiveJobDTO(
                                        task.getId(),
                                        "EXTERNAL_TASK",
                                        task.getProcessInstanceId(),
                                        task.getActivityId(),
                                        task.getLockExpirationTime(),
                                        "Topic: " + task.getTopicName() + ", Worker: " + task.getWorkerId()));
                }

                // 2. Scheduled Jobs (Timers)
                List<JobEntity> timerJobs = jobRepository.findAll();
                for (JobEntity job : timerJobs) {
                        activeJobs.add(new ActiveJobDTO(
                                        job.getId(),
                                        "TIMER",
                                        job.getProcessInstanceId(),
                                        job.getEventId(),
                                        job.getExecutionTimestamp(),
                                        "Scheduled execution"));
                }

                // 3. Waiting Message/Signal Events
                for (ProcessInstance pi : abadaEngine.getAllProcessInstances()) {
                        ParsedProcessDefinition def = pi.getDefinition();
                        for (String tokenId : pi.getActiveTokens()) {
                                if (def.isCatchEvent(tokenId)) {
                                        EventMeta event = def.getEvents().get(tokenId);
                                        if (event != null && (event.type() == EventMeta.EventType.MESSAGE
                                                        || event.type() == EventMeta.EventType.SIGNAL)) {
                                                activeJobs.add(new ActiveJobDTO(
                                                                pi.getId() + "_" + tokenId, // Synthetic ID
                                                                event.type().name(),
                                                                pi.getId(),
                                                                tokenId,
                                                                null, // No specific scheduled time
                                                                "Waiting for " + event.type().name() + ": "
                                                                                + event.definitionRef()));
                                        }
                                }
                        }
                }

                return ResponseEntity.ok(activeJobs);
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
