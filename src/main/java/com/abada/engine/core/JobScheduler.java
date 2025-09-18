package com.abada.engine.core;

import com.abada.engine.persistence.entity.JobEntity;
import com.abada.engine.persistence.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

    private final JobRepository jobRepository;
    private AbadaEngine abadaEngine;

    public JobScheduler(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    // Using setter injection to resolve circular dependency with AbadaEngine
    public void setAbadaEngine(AbadaEngine abadaEngine) {
        this.abadaEngine = abadaEngine;
    }

    /**
     * Creates and persists a new job to be executed at a specific time.
     */
    public void scheduleJob(String processInstanceId, String eventId, Instant executionTimestamp) {
        JobEntity job = new JobEntity(processInstanceId, eventId, executionTimestamp);
        jobRepository.save(job);
        log.info("Scheduled job {} for instance {} at {}", job.getId(), processInstanceId, executionTimestamp);
    }

    /**
     * Periodically polls the database for due jobs.
     */
    @Scheduled(fixedDelay = 60000) // Check every 60 seconds
    public void executeDueJobs() {
        if (abadaEngine == null) {
            // Engine may not be ready during initial startup
            return;
        }
        log.debug("Checking for due jobs...");
        List<JobEntity> dueJobs = jobRepository.findByExecutionTimestampLessThanEqual(Instant.now());

        if (dueJobs.isEmpty()) {
            return;
        }

        log.info("Found {} due jobs to execute.", dueJobs.size());

        for (JobEntity job : dueJobs) {
            try {
                abadaEngine.resumeFromEvent(job.getProcessInstanceId(), job.getEventId(), Map.of());
                jobRepository.delete(job);
                log.info("Executed and deleted job {}", job.getId());
            } catch (Exception e) {
                log.error("Failed to execute job {}: {}", job.getId(), e.getMessage(), e);
            }
        }
    }
}
