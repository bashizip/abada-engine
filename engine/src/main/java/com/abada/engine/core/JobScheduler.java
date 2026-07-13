package com.abada.engine.core;

import com.abada.engine.observability.EngineMetrics;
import com.abada.engine.persistence.entity.JobEntity;
import com.abada.engine.persistence.repository.JobRepository;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.annotation.SpanTag;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;

@Component
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

    private final JobRepository jobRepository;
    private final EngineMetrics engineMetrics;
    private final Tracer tracer;
    private AbadaEngine abadaEngine;
    private final String leaseOwner = UUID.randomUUID().toString();

    @Autowired
    public JobScheduler(JobRepository jobRepository, EngineMetrics engineMetrics, Tracer tracer) {
        this.jobRepository = jobRepository;
        this.engineMetrics = engineMetrics;
        this.tracer = tracer;
    }

    // Using setter injection to resolve circular dependency with AbadaEngine
    public void setAbadaEngine(AbadaEngine abadaEngine) {
        this.abadaEngine = abadaEngine;
    }

    /**
     * Creates and persists a new job to be executed at a specific time.
     */
    @WithSpan("abada.job.schedule")
    public void scheduleJob(@SpanTag("process.instance.id") String processInstanceId, 
                           @SpanTag("event.id") String eventId, 
                           @SpanTag("execution.timestamp") Instant executionTimestamp) {
        Span span = tracer.spanBuilder("abada.job.schedule").startSpan();
        
        try (var scope = span.makeCurrent()) {
            if (jobRepository.existsByProcessInstanceIdAndEventIdAndStatusIn(processInstanceId, eventId,
                    List.of(JobEntity.Status.AVAILABLE, JobEntity.Status.LEASED))) {
                return;
            }
            JobEntity job = new JobEntity(processInstanceId, eventId, executionTimestamp);
            jobRepository.save(job);
            
            span.setAttribute("job.id", job.getId());
            span.setAttribute("process.instance.id", processInstanceId);
            span.setAttribute("event.id", eventId);
            span.setAttribute("execution.timestamp", executionTimestamp.toString());
            span.setAttribute("job.type", "TIMER");
            
            log.info("Scheduled job {} for instance {} at {}", job.getId(), processInstanceId, executionTimestamp);
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Periodically polls the database for due jobs.
     */
    @Scheduled(fixedDelay = 60000) // Check every 60 seconds
    @WithSpan("abada.job.execute.due")
    @Transactional
    public void executeDueJobs() {
        Span span = tracer.spanBuilder("abada.job.execute.due").startSpan();
        
        try (var scope = span.makeCurrent()) {
            if (abadaEngine == null) {
                // Engine may not be ready during initial startup
                return;
            }
            log.debug("Checking for due jobs...");
            Instant now = Instant.now();
            List<JobEntity> dueJobs = new ArrayList<>(jobRepository
                    .findByStatusAndExecutionTimestampLessThanEqualOrderByExecutionTimestampAsc(
                            JobEntity.Status.AVAILABLE, now));
            dueJobs.addAll(jobRepository.findByStatusAndLeaseExpiresAtLessThanEqualOrderByExecutionTimestampAsc(
                    JobEntity.Status.LEASED, now));

            span.setAttribute("due.jobs.count", dueJobs.size());

            if (dueJobs.isEmpty()) {
                return;
            }

            log.info("Found {} due jobs to execute.", dueJobs.size());

            for (JobEntity job : dueJobs) {
                Timer.Sample sample = engineMetrics.startJobExecutionTimer();
                Span jobSpan = tracer.spanBuilder("abada.job.execute").startSpan();
                
                try (var jobScope = jobSpan.makeCurrent()) {
                    job.setStatus(JobEntity.Status.LEASED);
                    job.setLeaseOwner(leaseOwner);
                    job.setLeaseExpiresAt(Instant.now().plusSeconds(120));
                    job.setAttempts(job.getAttempts() + 1);
                    jobRepository.saveAndFlush(job);
                    jobSpan.setAttribute("job.id", job.getId());
                    jobSpan.setAttribute("process.instance.id", job.getProcessInstanceId());
                    jobSpan.setAttribute("event.id", job.getEventId());
                    jobSpan.setAttribute("job.type", "TIMER");
                    
                    abadaEngine.resumeFromEvent(job.getProcessInstanceId(), job.getEventId(), Map.of());
                    job.setStatus(JobEntity.Status.COMPLETED);
                    job.setLeaseOwner(null);
                    job.setLeaseExpiresAt(null);
                    jobRepository.save(job);
                    
                    engineMetrics.recordJobExecuted("TIMER");
                    engineMetrics.recordJobExecutionTime(sample, "TIMER");
                    
                    log.info("Executed and deleted job {}", job.getId());
                } catch (Exception e) {
                    engineMetrics.recordJobFailed("TIMER");
                    jobSpan.recordException(e);
                    jobSpan.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
                    log.error("Failed to execute job {}: {}", job.getId(), e.getMessage(), e);
                    job.setLastError(e.getMessage());
                    job.setLeaseOwner(null);
                    job.setLeaseExpiresAt(null);
                    job.setStatus(job.getAttempts() >= job.getMaxAttempts()
                            ? JobEntity.Status.FAILED : JobEntity.Status.AVAILABLE);
                    jobRepository.save(job);
                } finally {
                    jobSpan.end();
                }
            }
        } finally {
            span.end();
        }
    }
}
