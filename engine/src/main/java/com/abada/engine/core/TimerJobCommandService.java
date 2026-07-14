package com.abada.engine.core;

import com.abada.engine.persistence.entity.JobEntity;
import com.abada.engine.persistence.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;

import java.time.Instant;
import java.util.Map;

@Service
public class TimerJobCommandService {
    private final JobRepository repository;
    private final AbadaEngine engine;
    private final ActivityHistoryService history;

    public TimerJobCommandService(JobRepository repository, @Lazy AbadaEngine engine, ActivityHistoryService history) {
        this.repository = repository;
        this.engine = engine;
        this.history = history;
    }

    /** Executes one timer and its workflow advancement in a single transaction. */
    @AtomicRuntimeCommand
    public boolean execute(String jobId, String leaseOwner, Instant now) {
        JobEntity job = repository.findByIdForUpdate(jobId).orElse(null);
        if (job == null || !isClaimable(job, now)) return false;

        job.setStatus(JobEntity.Status.LEASED);
        job.setLeaseOwner(leaseOwner);
        job.setLeaseExpiresAt(now.plusSeconds(120));
        job.setAttempts(job.getAttempts() + 1);
        repository.save(job);

        engine.resumeFromEvent(job.getProcessInstanceId(), job.getEventId(), Map.of());
        job.setStatus(JobEntity.Status.COMPLETED);
        job.setLeaseOwner(null);
        job.setLeaseExpiresAt(null);
        repository.save(job);

        ProcessInstance instance = engine.getProcessInstanceById(job.getProcessInstanceId());
        history.record("TIMER_JOB_COMPLETED", instance, job.getEventId(), Map.of("jobId", jobId));
        return true;
    }

    /** Records retry state only after execute() has rolled its transaction back. */
    @AtomicRuntimeCommand
    public void recordFailure(String jobId, String error) {
        JobEntity job = repository.findByIdForUpdate(jobId).orElse(null);
        if (job == null || job.getStatus() == JobEntity.Status.COMPLETED) return;

        job.setAttempts(job.getAttempts() + 1);
        job.setLastError(error);
        job.setLeaseOwner(null);
        job.setLeaseExpiresAt(null);
        job.setStatus(job.getAttempts() >= job.getMaxAttempts()
                ? JobEntity.Status.FAILED : JobEntity.Status.AVAILABLE);
        repository.save(job);
        ProcessInstance instance = engine.getProcessInstanceById(job.getProcessInstanceId());
        history.record("TIMER_JOB_FAILED", instance, job.getEventId(),
                Map.of("jobId", jobId, "attempts", job.getAttempts(), "error", error == null ? "" : error));
    }

    private boolean isClaimable(JobEntity job, Instant now) {
        if (job.getStatus() == JobEntity.Status.AVAILABLE) {
            return !job.getExecutionTimestamp().isAfter(now);
        }
        return job.getStatus() == JobEntity.Status.LEASED
                && job.getLeaseExpiresAt() != null && !job.getLeaseExpiresAt().isAfter(now);
    }
}
